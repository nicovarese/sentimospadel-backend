package com.sentimospadel.backend.auth.service;

import com.sentimospadel.backend.auth.config.EmailVerificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationNotificationService {

    private final EmailVerificationProperties properties;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

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
            throw new IllegalStateException("Email verification delivery requires a configured JavaMailSender");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFromAddress());
        message.setTo(email);
        message.setSubject(properties.getSubject());
        message.setText(buildEmailBody(displayName, verificationUrl));

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new IllegalStateException("Email verification delivery failed", exception);
        }
    }

    private String buildEmailBody(String displayName, String verificationUrl) {
        return """
                Hola %s,

                Gracias por registrarte en Sentimos Padel.

                Para confirmar tu correo, hace click en este link:
                %s

                Si vos no creaste esta cuenta, podés ignorar este mensaje.
                """.formatted(displayName, verificationUrl);
    }
}
