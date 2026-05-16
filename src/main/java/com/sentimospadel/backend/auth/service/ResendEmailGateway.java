package com.sentimospadel.backend.auth.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Sends emails through Resend's HTTPS API (api.resend.com). We use this instead of SMTP
// because most managed PaaS providers (Railway included) block outbound port 587 to
// prevent spam abuse, while port 443 always works. Uses java.net.http.HttpClient to
// avoid pulling in additional dependencies.
@Component
@Slf4j
public class ResendEmailGateway {

    private static final URI RESEND_URL = URI.create("https://api.resend.com/emails");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final String apiKey;

    public ResendEmailGateway(@Value("${app.mail.resend.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public void send(String from, String to, String subject, String text) {
        if (!isConfigured()) {
            throw new IllegalStateException("RESEND_API_KEY is not configured");
        }

        String body = buildJsonPayload(from, to, subject, text);

        HttpRequest request = HttpRequest.newBuilder(RESEND_URL)
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.io.IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Resend delivery failed: " + exception.getMessage(), exception);
        }

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return;
        }

        throw new IllegalStateException(
                "Resend rejected the request (HTTP " + response.statusCode() + "): " + response.body()
        );
    }

    // Tiny hand-rolled JSON so we don't pull in Jackson for a 4-field payload. Resend's
    // schema is stable enough to make this trivial. The strings we put in here are the
    // verification email body which we control end-to-end.
    private String buildJsonPayload(String from, String to, String subject, String text) {
        return "{"
                + "\"from\":\"" + escape(from) + "\","
                + "\"to\":\"" + escape(to) + "\","
                + "\"subject\":\"" + escape(subject) + "\","
                + "\"text\":\"" + escape(text) + "\""
                + "}";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
