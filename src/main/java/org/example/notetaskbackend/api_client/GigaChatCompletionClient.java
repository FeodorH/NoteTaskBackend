package org.example.notetaskbackend.api_client;

import lombok.extern.slf4j.Slf4j;
import org.example.notetaskbackend.config.GigaChatProperties;
import org.example.notetaskbackend.dto.gigachat_api.GigaChatCompletionRequest;
import org.example.notetaskbackend.dto.gigachat_api.GigaChatCompletionResponse;
import org.example.notetaskbackend.dto.gigachat_api.GigaChatMessage;
import org.example.notetaskbackend.exception.GigaChatUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class GigaChatCompletionClient {
    WebClient webClient;
    GigaChatProperties properties;

    public GigaChatCompletionClient(@Qualifier("gigaChatWebClient") WebClient webClient,
                                    GigaChatProperties properties){
        this.webClient = webClient;
        this.properties = properties;
    }

    public Mono<String> complete(String prompt, String system, String token){
        List<GigaChatMessage> messages = new ArrayList<>();
        if (system != null && !system.isBlank()) {
            messages.add(new GigaChatMessage("system", system));
        }

        messages.add(new GigaChatMessage("user",
                "Преобразуй следующий текст в чёткую задачу. Верни только текст задачи, без лишних слов, кратко. Текст: "+prompt));

        GigaChatCompletionRequest request = new GigaChatCompletionRequest(properties.model(),messages);

        return webClient.post()
                .uri(properties.completionPath())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GigaChatCompletionResponse.class)
                .map(resp -> {
                    if (resp.choices() == null || resp.choices().isEmpty()) {
                        throw new GigaChatUnavailableException("GigaChat returned empty response");
                    }
                    return resp.choices().get(0).message().content();
                });
    }
}
