package org.example.notetaskbackend.token_provider;

import org.example.notetaskbackend.auth_client.GigaChatAuthClient;
import org.example.notetaskbackend.dto.gigachat_api.GigaChatAuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenProviderTest {

    @Mock
    private GigaChatAuthClient authClient;

    private GigaChatTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new GigaChatTokenProvider(authClient);
    }

    private GigaChatAuthResponse validResponse(String token) {
        return new GigaChatAuthResponse(token, System.currentTimeMillis() + 1_800_000L);
    }

    @Test
    @DisplayName("Пустой кэш → вызывает authClient, кэширует токен")
    void cacheEmpty_callsAuthClient() {
        when(authClient.requestToken()).thenReturn(Mono.just(validResponse("token-1")));

        StepVerifier.create(tokenProvider.getToken())
                .expectNext("token-1")
                .verifyComplete();

        verify(authClient, times(1)).requestToken();
    }

    @Test
    @DisplayName("Валидный кэш → возвращает токен без вызова authClient")
    void cacheValid_doesNotCallAuthClient() {
        when(authClient.requestToken()).thenReturn(Mono.just(validResponse("token-1")));

        tokenProvider.getToken().block();

        StepVerifier.create(tokenProvider.getToken())
                .expectNext("token-1")
                .verifyComplete();

        verify(authClient, times(1)).requestToken();   // всё ещё 1
    }

    @Test
    @DisplayName("invalidate() → следующий запрос идёт за новым токеном")
    void invalidate_clearsCache() {
        when(authClient.requestToken())
                .thenReturn(Mono.just(validResponse("token-1")))
                .thenReturn(Mono.just(validResponse("token-2")));

        tokenProvider.getToken().block();
        tokenProvider.invalidate();

        StepVerifier.create(tokenProvider.getToken())
                .expectNext("token-2")
                .verifyComplete();

        verify(authClient, times(2)).requestToken();
    }

    @Test
    @DisplayName("Параллельные запросы с пустым кэшем → один вызов authClient (single-flight)")
    void parallelRequests_singleFlight() {
        AtomicInteger calls = new AtomicInteger();
        when(authClient.requestToken()).thenAnswer(inv -> {
            calls.incrementAndGet();
            return Mono.just(validResponse("token-shared"))
                    .delayElement(java.time.Duration.ofMillis(100));
        });

        // Два параллельных подписчика на один Mono
        Mono<String> shared = tokenProvider.getToken();

        StepVerifier.create(Mono.zip(shared, shared))
                .expectNextMatches(t -> t.getT1().equals("token-shared")
                        && t.getT2().equals("token-shared"))
                .verifyComplete();

        assertEquals(1, calls.get(), "authClient должен быть вызван один раз");
    }

    @Test
    @DisplayName("Ошибка authClient → пробрасывается подписчику")
    void authError_propagated() {
        when(authClient.requestToken())
                .thenReturn(Mono.error(new RuntimeException("auth failed")));

        StepVerifier.create(tokenProvider.getToken())
                .expectErrorMatches(e -> e.getMessage().equals("auth failed"))
                .verify();
    }
}