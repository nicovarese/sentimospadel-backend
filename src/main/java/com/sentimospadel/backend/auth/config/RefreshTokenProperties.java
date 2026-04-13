package com.sentimospadel.backend.auth.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.auth.refresh-token")
public class RefreshTokenProperties {

    private Duration expiration = Duration.ofDays(30);
}
