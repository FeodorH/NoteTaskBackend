package org.example.notetaskbackend.security;

import lombok.RequiredArgsConstructor;
import org.example.notetaskbackend.config.AppProperties;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationManager implements ReactiveAuthenticationManager {

    private final AppProperties appProperties;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String presented = (String) authentication.getCredentials();
        if (presented != null && presented.equals(appProperties.apiKey())) {
            return Mono.just(new ApiKeyAuthenticationToken(
                    "api-client",
                    List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))
            ));
        }
        return Mono.error(new BadCredentialsException("Invalid API key"));
    }
}