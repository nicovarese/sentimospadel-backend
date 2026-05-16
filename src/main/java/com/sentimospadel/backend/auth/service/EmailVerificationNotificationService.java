package com.sentimospadel.backend.auth.service;

import com.sentimospadel.backend.auth.config.EmailVerificationProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationNotificationService {

    private final EmailVerificationProperties properties;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @PostConstruct
    void validateConfiguration() {
        if (properties.isLogOnly()) {
            return;
        }

        requireNonBlank(properties.getVerificationBaseUrl(), "EMAIL_VERIFICATION_BASE_URL");
        requireNonBlank(properties.getLoginUrl(), "EMAIL_VERIFICATION_LOGIN_URL");
        requireNonBlank(properties.getFromAddress(), "EMAIL_VERIFICATION_FROM");
    }

    // Async so a slow SMTP server (timeouts, throttling, etc.) never blocks the HTTP
    // response to /api/auth/register. If delivery fails we log it; the user can recover
    // via /api/auth/verify-email/resend.
    @Async
    public void sendVerificationEmail(String email, String displayName, String rawToken) {
        String verificationUrl = UriComponentsBuilder
                .fromUriString(properties.getVerificationBaseUrl())
                .queryParam("token", rawToken)
                .build(true)
                .toUriString();

        if (properties.isLogOnly()) {
            log.info("Email verification link for {} <{}>: {}", displayName, email, verificationUrl);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.error("Email verification delivery requires a configured JavaMailSender; cannot deliver to {}", email);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFromAddress());
        message.setTo(email);
        message.setSubject(properties.getSubject());
        message.setText(buildEmailBody(displayName, verificationUrl));

        try {
            mailSender.send(message);
            log.info("Sent verification email to {}", email);
        } catch (MailException exception) {
            log.error("Email verification delivery failed for {}", email, exception);
        }
    }

    private String buildEmailBody(String displayName, String verificationUrl) {
        return """
                Hola %s,

                Gracias por registrarte en Sentimos Padel.

                Para confirmar tu correo, hace click en este link:
                %s

                Si vos no creaste esta cuenta, podes ignorar este mensaje.
                """.formatted(displayName, verificationUrl);
    }

    private void requireNonBlank(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " is required when EMAIL_VERIFICATION_LOG_ONLY=false");
        }
    }
}
