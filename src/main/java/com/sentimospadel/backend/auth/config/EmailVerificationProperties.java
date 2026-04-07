package com.sentimospadel.backend.auth.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.auth.email-verification")
public class EmailVerificationProperties {

    private Duration expiration = Duration.ofHours(24);
    private boolean logOnly = true;
    private String verificationBaseUrl = "http://localhost:8080/api/auth/verify-email";
    private String fromAddress = "no-reply@sentimospadel.test";
    private String loginUrl = "http://localhost:3000";
    private String subject = "Confirma tu email en Sentimos Padel";
}
