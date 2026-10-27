package org.example.notetaskbackend.service;

import org.example.notetaskbackend.api_client.GigaChatCompletionClient;
import org.example.notetaskbackend.exception.GigaChatUnavailableException;
import org.example.notetaskbackend.token_provider.GigaChatTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GigaChatServiceTest {

    @Mock
    private GigaChatTokenProvider tokenProvider;

    @Mock
    private GigaChatCompletionClient completionClient;

    private GigaChatService service;

    @BeforeEach
    void setUp() {
        service = new GigaChatService(tokenProvider, completionClient);
    }

    @Test
    @DisplayName("Успешный вызов → возвращает результат")
    void success() {
        when(tokenProvider.getToken()).thenReturn(Mono.just("token-1"));
        when(completionClient.complete(eq("test prompt"), anyString(), eq("token-1")))
                .thenReturn(Mono.just("generated result"));

        StepVerifier.create(service.generateTask("test prompt"))
                .expectNext("generated result")
                .verifyComplete();

        verify(completionClient, times(1)).complete(anyString(), anyString(), eq("token-1"));
        verify(tokenProvider, never()).invalidate();
    }

    @Test
    @DisplayName("401 → invalidate + retry с новым токеном")
    void unauthorized_retriesWithFreshToken() {
        when(tokenProvider.getToken())
                .thenReturn(Mono.just("token-old"))
                .thenReturn(Mono.just("token-new"));

        WebClientResponseException unauthorized = WebClientResponseException.create(
                401, "Unauthorized", null, null, null);

        when(completionClient.complete(anyString(), anyString(), eq("token-old")))
                .thenReturn(Mono.error(unauthorized));
        when(completionClient.complete(anyString(), anyString(), eq("token-new")))
                .thenReturn(Mono.just("result after retry"));

        StepVerifier.create(service.generateTask("test prompt"))
                .expectNext("result after retry")
                .verifyComplete();

        verify(tokenProvider, times(1)).invalidate();
        verify(completionClient, times(2)).complete(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Повторный 401 → пробрасывает GigaChatUnavailableException")
    void secondUnauthorized_throwsUnavailable() {
        when(tokenProvider.getToken()).thenReturn(Mono.just("token-old"));
        when(tokenProvider.getToken()).thenReturn(Mono.just("token-new"));

        WebClientResponseException unauthorized = WebClientResponseException.create(
                401, "Unauthorized", null, null, null);

        when(completionClient.complete(anyString(), anyString(), anyString()))
                .thenReturn(Mono.error(unauthorized));

        StepVerifier.create(service.generateTask("test prompt"))
                .expectError(GigaChatUnavailableException.class)
                .verify();
    }

    @Test
    @DisplayName("Сетевая ошибка → GigaChatUnavailableException")
    void networkError_mapsToUnavailable() {
        when(tokenProvider.getToken()).thenReturn(Mono.just("token-1"));
        when(completionClient.complete(anyString(), anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("connection refused")));

        StepVerifier.create(service.generateTask("test prompt"))
                .expectErrorMatches(e -> e instanceof GigaChatUnavailableException
                        && e.getMessage().contains("connection refused"))
                .verify();
    }

    @Test
    @DisplayName("Ошибка получения токена → GigaChatUnavailableException")
    void tokenError_mapsToUnavailable() {
        when(tokenProvider.getToken())
                .thenReturn(Mono.error(new RuntimeException("auth failed")));

        StepVerifier.create(service.generateTask("test prompt"))
                .expectError(GigaChatUnavailableException.class)
                .verify();
    }
}