package com.sentimospadel.backend.auth.ratelimit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sentimospadel.backend.auth.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class AuthRateLimiterTest {

    @Test
    void loginLimitIsAppliedByClientIpAndEmail() {
        AuthRateLimitProperties properties = new AuthRateLimitProperties();
        properties.getLogin().setMaxAttempts(2);
        AuthRateLimiter limiter = new AuthRateLimiter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        LoginRequest loginRequest = new LoginRequest("player@example.com", "password");

        limiter.checkLogin(request, loginRequest);
        limiter.checkLogin(request, loginRequest);

        assertThatThrownBy(() -> limiter.checkLogin(request, loginRequest))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessage("Demasiados intentos. Proba de nuevo mas tarde.");
    }

    @Test
    void disabledLimiterAllowsRequests() {
        AuthRateLimitProperties properties = new AuthRateLimitProperties();
        properties.setEnabled(false);
        properties.getLogin().setMaxAttempts(1);
        AuthRateLimiter limiter = new AuthRateLimiter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        LoginRequest loginRequest = new LoginRequest("player@example.com", "password");

        limiter.checkLogin(request, loginRequest);
        limiter.checkLogin(request, loginRequest);
        limiter.checkLogin(request, loginRequest);
    }
}
