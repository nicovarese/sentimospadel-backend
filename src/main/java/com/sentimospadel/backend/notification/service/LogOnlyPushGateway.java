package com.sentimospadel.backend.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentimospadel.backend.notification.config.NotificationPushProperties;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogOnlyPushGateway implements PushGateway {

    private static final String PROVIDER_FCM = "fcm";
    private static final String PROVIDER_LOG_ONLY = "log-only";
    private static final String FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final long ACCESS_TOKEN_REFRESH_SKEW_SECONDS = 60;

    private final NotificationPushProperties notificationPushProperties;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Clock clock = Clock.systemUTC();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private FcmAccessToken cachedAccessToken;

    @Override
    public String providerName() {
        String configuredProvider = notificationPushProperties.provider();
        if (configuredProvider == null || configuredProvider.isBlank()) {
            return PROVIDER_LOG_ONLY;
        }
        return configuredProvider.trim();
    }

    @PostConstruct
    void validateConfiguration() {
        if (!notificationPushProperties.enabled() || !PROVIDER_FCM.equalsIgnoreCase(providerName())) {
            return;
        }

        try {
            FcmServiceAccount serviceAccount = readServiceAccount();
            resolveProjectId(serviceAccount);
            requireText(serviceAccount.clientEmail(), "FCM service account client_email is required");
            requireText(serviceAccount.privateKey(), "FCM service account private_key is required");
            requireText(serviceAccount.tokenUri(), "FCM service account token_uri is required");
        } catch (Exception error) {
            throw new IllegalStateException("Invalid FCM push notification configuration: " + error.getMessage(), error);
        }
    }

    @Override
    public PushGatewayResult send(PushGatewayRequest request) {
        if (PROVIDER_FCM.equalsIgnoreCase(providerName())) {
            return sendFcm(request);
        }

        if (notificationPushProperties.logPayloads()) {
            log.info(
                    "Push delivery [{}] notification={} platform={} token={} type={} title=\"{}\"",
                    providerName(),
                    request.notificationId(),
                    request.platform(),
                    abbreviateToken(request.token()),
                    request.type(),
                    request.title()
            );
        }

        return new PushGatewayResult(true, "log-only-" + request.notificationId(), null);
    }

    private PushGatewayResult sendFcm(PushGatewayRequest request) {
        try {
            FcmServiceAccount serviceAccount = readServiceAccount();
            String projectId = resolveProjectId(serviceAccount);
            String accessToken = getAccessToken(serviceAccount);
            Map<String, Object> body = buildFcmMessage(request);
            String responseBody = postJson(
                    String.format(notificationPushProperties.fcmEndpoint(), projectId),
                    accessToken,
                    objectMapper.writeValueAsString(body)
            );
            JsonNode response = objectMapper.readTree(responseBody);
            String messageName = response.path("name").asText(null);
            return new PushGatewayResult(true, messageName, null);
        } catch (Exception error) {
            log.warn(
                    "FCM push delivery failed for notification={} platform={} token={}",
                    request.notificationId(),
                    request.platform(),
                    abbreviateToken(request.token()),
                    error
            );
            return new PushGatewayResult(false, null, "FCM_SEND_ERROR: " + error.getMessage());
        }
    }

    private FcmServiceAccount readServiceAccount() throws IOException {
        String encoded = notificationPushProperties.fcmServiceAccountJsonBase64();
        if (!StringUtils.hasText(encoded)) {
            throw new IllegalStateException("FCM service account is not configured");
        }

        String json = new String(Base64.getDecoder().decode(encoded.trim()), StandardCharsets.UTF_8);
        JsonNode root = objectMapper.readTree(json);
        return new FcmServiceAccount(
                textValue(root, "project_id"),
                textValue(root, "client_email"),
                textValue(root, "private_key"),
                StringUtils.hasText(textValue(root, "token_uri"))
                        ? textValue(root, "token_uri")
                        : notificationPushProperties.fcmAccessTokenUri()
        );
    }

    private String getAccessToken(FcmServiceAccount serviceAccount) throws IOException, InterruptedException, GeneralSecurityException {
        Instant now = clock.instant();
        if (cachedAccessToken != null && cachedAccessToken.expiresAt().isAfter(now.plusSeconds(ACCESS_TOKEN_REFRESH_SKEW_SECONDS))) {
            return cachedAccessToken.token();
        }

        String assertion = buildAccessTokenAssertion(serviceAccount, now);
        String formBody = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", StandardCharsets.UTF_8)
                + "&assertion=" + URLEncoder.encode(assertion, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder(URI.create(serviceAccount.tokenUri()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("FCM access token request failed with HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String token = textValue(root, "access_token");
        long expiresInSeconds = root.path("expires_in").asLong(3600);
        if (!StringUtils.hasText(token)) {
            throw new IllegalStateException("FCM access token response did not include access_token");
        }

        cachedAccessToken = new FcmAccessToken(token, now.plusSeconds(expiresInSeconds));
        return cachedAccessToken.token();
    }

    private String buildAccessTokenAssertion(FcmServiceAccount serviceAccount, Instant now) throws GeneralSecurityException {
        PrivateKey privateKey = parsePrivateKey(serviceAccount.privateKey());
        return Jwts.builder()
                .issuer(serviceAccount.clientEmail())
                .subject(serviceAccount.clientEmail())
                .audience().add(serviceAccount.tokenUri()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .claim("scope", FCM_SCOPE)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    private PrivateKey parsePrivateKey(String pem) throws GeneralSecurityException {
        if (!StringUtils.hasText(pem)) {
            throw new IllegalStateException("FCM service account private_key is missing");
        }

        String normalized = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    private String postJson(String url, String accessToken, String jsonBody) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("FCM send failed with HTTP " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    Map<String, Object> buildFcmMessage(PushGatewayRequest request) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("notificationId", String.valueOf(request.notificationId()));
        data.put("type", request.type().name());
        putNullable(data, "matchId", request.matchId());
        putNullable(data, "tournamentId", request.tournamentId());
        putNullable(data, "tournamentMatchId", request.tournamentMatchId());

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("token", request.token());
        message.put("notification", Map.of(
                "title", request.title(),
                "body", request.message()
        ));
        message.put("data", data);
        message.put("android", Map.of("priority", "HIGH"));
        message.put("apns", Map.of("payload", Map.of("aps", Map.of("sound", "default"))));

        return Map.of("message", message);
    }

    private void putNullable(Map<String, String> data, String key, Long value) {
        if (value != null) {
            data.put(key, String.valueOf(value));
        }
    }

    private String resolveProjectId(FcmServiceAccount serviceAccount) {
        if (StringUtils.hasText(notificationPushProperties.fcmProjectId())) {
            return notificationPushProperties.fcmProjectId().trim();
        }
        if (StringUtils.hasText(serviceAccount.projectId())) {
            return serviceAccount.projectId().trim();
        }
        throw new IllegalStateException("FCM_PROJECT_ID is required when service account does not include project_id");
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(message);
        }
    }

    private String textValue(JsonNode root, String field) {
        JsonNode value = root.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private String abbreviateToken(String token) {
        if (token == null || token.length() <= 12) {
            return token;
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }

    private record FcmServiceAccount(
            String projectId,
            String clientEmail,
            String privateKey,
            String tokenUri
    ) {
    }

    private record FcmAccessToken(
            String token,
            Instant expiresAt
    ) {
    }
}
