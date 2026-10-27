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
    private final AtomicReference<Mono<String>> inFlight = new AtomicReference<>(); // Временный кеш для обновления в полёте

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
            Mono<String> existing = inFlight.get();
            if (existing != null) {
                return existing;
            }

            Mono<String> fresh = authClient.requestToken()
                    .map(resp -> {
                        Instant expiresAt = Instant.now().plusSeconds(resp.expiresIn());
                        CachedToken cached = new CachedToken(resp.accessToken(), expiresAt);
                        cache.set(cached);
                        inFlight.set(null);
                        log.info("GigaChat token cached, expires at {}", expiresAt);
                        return resp.accessToken();
                    })
                    .doOnError(e -> {
                        inFlight.set(null);
                        log.error("Token fetch failed", e);
                    })
                    .cache();

            if (inFlight.compareAndSet(null, fresh)) {
                return fresh;
            }
            return inFlight.get();
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