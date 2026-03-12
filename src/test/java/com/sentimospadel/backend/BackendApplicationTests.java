package com.sentimospadel.backend;

import com.sentimospadel.backend.config.security.SecurityConfig;
import com.sentimospadel.backend.shared.api.HealthController;
import com.sentimospadel.backend.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = BackendApplicationTests.TestApplication.class)
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({SecurityConfig.class, HealthController.class, GlobalExceptionHandler.class})
    static class TestApplication {
    }
}
