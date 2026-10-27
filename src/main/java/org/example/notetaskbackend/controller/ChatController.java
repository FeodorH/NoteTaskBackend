package org.example.notetaskbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.notetaskbackend.dto.ChatResponse;
import org.springframework.security.core.Authentication;
import org.example.notetaskbackend.service.GigaChatService;
import org.example.notetaskbackend.dto.ChatRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class ChatController {
    private final GigaChatService gigaChatService;

    @PostMapping("/generate")
    public Mono<ChatResponse> postGenerateRequest(
            @Valid @RequestBody ChatRequest clientRequest,
            Authentication authentication) {
        return  gigaChatService.generateTask(clientRequest.prompt()).map(result -> new ChatResponse(result,"200"));
    }
}
