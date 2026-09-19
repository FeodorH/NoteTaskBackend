package org.example.notetaskbackend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ApiKeyAuthenticationConverter implements ServerAuthenticationConverter {

    public static final String HEADER = "X-API-Key";

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        String apiKey = exchange.getRequest().getHeaders().getFirst(HEADER);
        if (!StringUtils.hasText(apiKey)) {
            return Mono.empty();
        }
        return Mono.just(new ApiKeyAuthenticationToken(apiKey));
    }
}
