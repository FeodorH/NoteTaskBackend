package org.example.notetaskbackend.controller;

import org.example.notetaskbackend.security.ApiKeyAuthenticationConverter;
import org.example.notetaskbackend.security.ApiKeyAuthenticationManager;
import org.example.notetaskbackend.security.SecurityConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({
        SecurityConfig.class,
        ApiKeyAuthenticationConverter.class,
        ApiKeyAuthenticationManager.class
})
public class SecurityTestImport {
}
