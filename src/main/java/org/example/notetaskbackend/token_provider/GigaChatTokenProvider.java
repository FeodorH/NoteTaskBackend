package org.example.notetaskbackend.token_provider;

import lombok.extern.slf4j.Slf4j;
import org.example.notetaskbackend.auth_client.GigaChatAuthClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class GigaChatTokenProvider {

    private final GigaChatAuthClient authClient;
    private final AtomicReference<CachedToken> cache = new AtomicReference<>(); // Атомарный кеш

    public GigaChatTokenProvider(GigaChatAuthClient authClient) {
        this.authClient = authClient;
    }

    public Mono<String> getToken() {
        CachedToken current = cache.get();
        if (current != null && current.isValid()) {
            return Mono.just(current.token());
        }

        // Для избежания параллельного запроса 2-х токенов и гонки в кеше
        return Mono.defer(() -> {
            CachedToken existing = cache.get();
            if (existing != null && existing.isValid()) {
                return Mono.just(existing.token());
            }

            Mono<String> fresh = authClient.requestToken()
                    .map(resp -> {
                        Instant expiresAt = Instant.now().plusSeconds(resp.expiresIn());
                        CachedToken cached = new CachedToken(resp.accessToken(), expiresAt);
                        cache.set(cached);
                        log.info("GigaChat token cached, expires at {}", expiresAt);
                        return resp.accessToken();
                    })
                    .doOnError(e -> log.error("Token fetch failed", e))
                    .cache();   // ← ключевая строка

            return fresh;
        });
    }

    // В случае 401 от гигачата
    public void invalidate() {
        CachedToken old = cache.getAndSet(null);
        if (old != null) {
            log.warn("GigaChat token invalidated");
        }
    }

    private record CachedToken(String token, Instant expiresAt) {
        boolean isValid() {
            // Считаем валидным, если осталось > 60 секунд
            return Instant.now().isBefore(expiresAt.minusSeconds(60));
        }
    }
}