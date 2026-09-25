package org.example.notetaskbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gigachat")
public record GigaChatProperties(
        String authUrl,
        String authPath,
        String apiUrl,
        String completionPath,
        String clientId,
        String clientSecret,
        String scope,
        String model
) {}
