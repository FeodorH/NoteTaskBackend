package org.example.notetaskbackend.config;

import org.example.notetaskbackend.config.AppProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestSecurityConfig {

    public static final String TEST_API_KEY = "test-api-key";

    @Bean
    public AppProperties appProperties() {
        return new AppProperties(TEST_API_KEY);
    }
}