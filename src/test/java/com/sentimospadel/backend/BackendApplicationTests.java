package com.sentimospadel.backend;

import com.sentimospadel.backend.auth.service.JwtService;
import com.sentimospadel.backend.config.security.JwtAuthenticationFilter;
import com.sentimospadel.backend.config.security.SecurityConfig;
import com.sentimospadel.backend.shared.api.HealthController;
import com.sentimospadel.backend.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = BackendApplicationTests.TestApplication.class)
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

    @TestConfiguration
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
    })
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
