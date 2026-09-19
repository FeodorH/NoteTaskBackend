package org.example.notetaskbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.notetaskbackend.dto.ClientResponse;
import org.springframework.security.core.Authentication;
import org.example.notetaskbackend.service.GigaChatService;
import org.example.notetaskbackend.dto.ClientRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/notetask/v1")
@RequiredArgsConstructor
public class ChatController {
    private final GigaChatService gigaChatService;

    @PostMapping("/generate")
    public Mono<ClientResponse> postGenerateRequest(
            @Valid @RequestBody ClientRequest clientRequest,
            Authentication authentication) {
        Mono<String> result = gigaChatService.generateTask(clientRequest.prompt());
        return result.map(s -> new ClientResponse(s,"200"));
    }
}
