package org.example.notetaskbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notetaskbackend.api_client.GigaChatCompletionClient;
import org.example.notetaskbackend.exception.GigaChatUnavailableException;
import org.example.notetaskbackend.token_provider.GigaChatTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class GigaChatService {

    private final GigaChatTokenProvider gigaChatTokenProvider;
    private final GigaChatCompletionClient gigaChatCompletionClient;

    public Mono<String> generateTask(String prompt) {
        String annot = "Ты помощник, который преобразует текст в задачу. ";

        return gigaChatTokenProvider.getToken()
                .flatMap(token -> gigaChatCompletionClient
                        .complete(prompt, annot, token))
                .onErrorResume(WebClientResponseException.Unauthorized.class, e -> {
                    log.warn("GigaChat 401, invalidating token and retrying once");
                    gigaChatTokenProvider.invalidate();
                    return gigaChatTokenProvider.getToken()
                            .flatMap(newToken -> gigaChatCompletionClient
                                    .complete(prompt, annot, newToken));
                })
                .onErrorMap(
                        e -> !(e instanceof GigaChatUnavailableException),
                        e -> new GigaChatUnavailableException("GigaChat недоступен: " + e.getMessage(), e)
                );
    }

}
