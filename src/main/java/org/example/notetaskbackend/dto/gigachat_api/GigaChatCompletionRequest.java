package org.example.notetaskbackend.dto.gigachat_api;

import java.util.List;

public record GigaChatCompletionRequest(
        String model,
        List<GigaChatMessage> messages
) {}