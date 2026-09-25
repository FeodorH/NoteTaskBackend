package org.example.notetaskbackend.controller;

import com.github.dockerjava.api.exception.InternalServerErrorException;
import org.example.notetaskbackend.dto.ChatResponse;
import org.example.notetaskbackend.service.GigaChatService;
import org.example.notetaskbackend.config.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(ChatController.class)
@Import({TestSecurityConfig.class, SecurityTestImport.class})
class ChatControllerTest {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String VALID_KEY = TestSecurityConfig.TEST_API_KEY;
    private static final String ENDPOINT = "/v1/generate";
    private static final String HEALTH_PATH = "/notetask/actuator/health";

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private GigaChatService gigaChatService;

    // ────────── 200 OK ──────────

    @Test
    @DisplayName("POST с валидным API-ключом и prompt → 200 + тело ответа")
    void shouldReturn200WithValidApiKey() {
        when(gigaChatService.generateTask("hello"))
                .thenReturn(Mono.just("generated result"));

        webTestClient.post()
                .uri(ENDPOINT)
                .header(API_KEY_HEADER, VALID_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChatRequestJson("hello"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ChatResponse.class)
                .value(response -> {
                    assert response.response().equals("generated result");
                    assert response.status().equals("200");
                });
    }

    // ────────── 401 Unauthorized ──────────

    @Test
    @DisplayName("POST без API-ключа → 401")
    void shouldReturn401WithoutApiKey() {
        webTestClient.post()
                .uri(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChatRequestJson("hello"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("POST с неверным API-ключом → 401")
    void shouldReturn401WithInvalidApiKey() {
        webTestClient.post()
                .uri(ENDPOINT)
                .header(API_KEY_HEADER, "wrong-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChatRequestJson("hello"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ────────── 400 Bad Request ──────────

    @Test
    @DisplayName("POST с пустым prompt → 400 + сообщение валидации")
    void shouldReturn400WhenPromptBlank() {
        webTestClient.post()
                .uri(ENDPOINT)
                .header(API_KEY_HEADER, VALID_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChatRequestJson(""))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo("validation_error")
                .jsonPath("$.message").value(msg -> {
                    assert msg.toString().contains("prompt");
                });
    }

    @Test
    @DisplayName("POST с null prompt → 400")
    void shouldReturn400WhenPromptNull() {
        webTestClient.post()
                .uri(ENDPOINT)
                .header(API_KEY_HEADER, VALID_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo("validation_error");
    }

    @Test
    @DisplayName("POST с prompt длиннее 4000 символов → 400")
    void shouldReturn400WhenPromptTooLong() {
        String tooLong = "a".repeat(4001);

        webTestClient.post()
                .uri(ENDPOINT)
                .header(API_KEY_HEADER, VALID_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChatRequestJson(tooLong))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo("validation_error");
    }

    // ────────── 5xx ──────────

    @Test
    @DisplayName("Сервис падает → 500")
    void shouldReturn500WhenServiceFails() {
        when(gigaChatService.generateTask(anyString()))
                .thenReturn(Mono.error(new InternalServerErrorException("Falling down!")));

        webTestClient.post()
                .uri(ENDPOINT)
                .header(API_KEY_HEADER, VALID_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChatRequestJson("hello"))
                .exchange()
                .expectStatus().isEqualTo(500);
    }

    // ────────── Вспомогательный record для тела запроса ──────────

    record ChatRequestJson(String prompt) {}
}