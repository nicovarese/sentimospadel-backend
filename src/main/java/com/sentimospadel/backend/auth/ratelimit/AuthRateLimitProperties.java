package com.sentimospadel.backend.auth.ratelimit;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.auth.rate-limit")
public class AuthRateLimitProperties {

    private boolean enabled = true;
    private Rule login = new Rule(5, Duration.ofMinutes(10));
    private Rule register = new Rule(3, Duration.ofHours(1));
    private Rule resendVerification = new Rule(3, Duration.ofMinutes(15));

    @Getter
    @Setter
    public static class Rule {

        private int maxAttempts;
        private Duration window;

        public Rule() {
        }

        public Rule(int maxAttempts, Duration window) {
            this.maxAttempts = maxAttempts;
            this.window = window;
        }
    }
}
