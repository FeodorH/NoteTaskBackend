package org.example.notetaskbackend.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.example.notetaskbackend.api_client.GigaChatCompletionClient;
import org.example.notetaskbackend.config.GigaChatProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class GigaChatCompletionClientTest {

    private MockWebServer server;
    private GigaChatCompletionClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        String baseUrl = server.url("/").toString().replaceAll("/$", "");

        GigaChatProperties props = new GigaChatProperties(
                baseUrl, "/api/v2/oauth",
                baseUrl, "/v1/chat/completions",
                "id", "secret",
                "GIGACHAT_API_PERS",
                "GigaChat-3-Ultra"
        );

        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
        client = new GigaChatCompletionClient(webClient, props);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("Успешный ответ → возвращает content первого choice")
    void success() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "choices": [
                            { "message": { "role": "assistant", "content": "Позвонить маме" } }
                          ]
                        }
                        """));

        StepVerifier.create(client.complete("позвонить маме", "ты ассистент", "token-1"))
                .expectNext("Позвонить маме")
                .verifyComplete();

        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/v1/chat/completions", request.getPath());
        assertEquals("Bearer token-1", request.getHeader("Authorization"));

        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"role\":\"system\""));
        assertTrue(body.contains("\"role\":\"user\""));
        assertTrue(body.contains("\"model\":\"GigaChat-3-Ultra\""));
    }

    @Test
    @DisplayName("Пустой choices → GigaChatUnavailableException")
    void emptyChoices_throws() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"choices":[]}
                        """));

        StepVerifier.create(client.complete("test", "system", "token"))
                .expectError()
                .verify();
    }

    @Test
    @DisplayName("401 → WebClientResponseException.Unauthorized")
    void unauthorized() {
        server.enqueue(new MockResponse().setResponseCode(401));

        StepVerifier.create(client.complete("test", "system", "token"))
                .expectErrorMatches(e -> e instanceof
                        org.springframework.web.reactive.function.client.WebClientResponseException.Unauthorized)
                .verify();
    }
}