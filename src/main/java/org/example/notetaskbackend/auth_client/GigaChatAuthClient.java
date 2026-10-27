package org.example.notetaskbackend.auth_client;

import lombok.extern.slf4j.Slf4j;
import org.example.notetaskbackend.config.GigaChatProperties;
import org.example.notetaskbackend.dto.gigachat_api.GigaChatAuthResponse;
import org.example.notetaskbackend.exception.GigaChatAuthException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.UUID;

@Component
@Slf4j
public class GigaChatAuthClient {

    private final WebClient authWebClient;
    private final GigaChatProperties properties;

    public GigaChatAuthClient(
            @Qualifier("gigaChatAuthWebClient") WebClient authWebClient,
            GigaChatProperties props) {
        this.authWebClient = authWebClient;
        this.properties = props;
    }

    public Mono<GigaChatAuthResponse> requestToken() {
        String basic = Base64.getEncoder()
                .encodeToString((properties.clientId() + ":" + properties.clientSecret()).getBytes());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("scope", properties.scope());

        return authWebClient.post()
                .uri(properties.authPath())
                .header("Authorization", "Basic " + basic)
                .header("RqUID", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(GigaChatAuthResponse.class)
                .doOnSuccess(r -> log.info("GigaChat token obtained, expires in {}s", r.expiresIn()))
                .doOnError(e -> log.error("GigaChat auth failed", e))
                .onErrorMap(WebClientResponseException.class,
                        e -> new GigaChatAuthException("GigaChat auth error: " + e.getStatusCode(), e));
    }
}