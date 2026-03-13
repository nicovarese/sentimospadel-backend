package com.sentimospadel.backend;

import com.sentimospadel.backend.auth.service.JwtService;
import com.sentimospadel.backend.config.security.JwtAuthenticationFilter;
import com.sentimospadel.backend.config.security.SecurityConfig;
import com.sentimospadel.backend.shared.api.HealthController;
import com.sentimospadel.backend.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootTest(
        classes = BackendApplicationTests.TestApplication.class,
        properties = {
                "app.security.jwt.secret=change-this-local-jwt-secret-change-this-local-jwt-secret",
                "app.security.jwt.expiration-ms=3600000"
        }
)
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            SecurityConfig.class,
            JwtService.class,
            JwtAuthenticationFilter.class,
            HealthController.class,
            GlobalExceptionHandler.class
    })
    static class TestApplication {
    }
}
