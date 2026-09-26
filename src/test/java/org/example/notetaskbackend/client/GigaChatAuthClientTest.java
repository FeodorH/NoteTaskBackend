package org.example.notetaskbackend.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.example.notetaskbackend.auth_client.GigaChatAuthClient;
import org.example.notetaskbackend.config.GigaChatProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class GigaChatAuthClientTest {

    private MockWebServer server;
    private GigaChatAuthClient client;
    private GigaChatProperties props;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        String baseUrl = server.url("/").toString().replaceAll("/$", "");

        props = new GigaChatProperties(
                baseUrl,             // auth-url
                "/api/v2/oauth",
                baseUrl,             // api-url
                "/v1/chat/completions",
                "test-client-id",
                "test-client-secret",
                "GIGACHAT_API_PERS",
                "GigaChat-3-Ultra"
        );

        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
        client = new GigaChatAuthClient(webClient, props);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("Успешный OAuth → парсит access_token и expires_at")
    void success() throws InterruptedException {
        long expiresAt = System.currentTimeMillis() + 1_800_000L;
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "access_token": "test-access-token",
                          "expires_at": %d
                        }
                        """.formatted(expiresAt)));

        StepVerifier.create(client.requestToken())
                .assertNext(resp -> {
                    assertEquals("test-access-token", resp.accessToken());
                    assertEquals(expiresAt, resp.expiresIn());
                })
                .verifyComplete();

        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/api/v2/oauth", request.getPath());

        // Проверяем Basic Auth
        String expectedBasic = Base64.getEncoder()
                .encodeToString("test-client-id:test-client-secret".getBytes());
        assertEquals("Basic " + expectedBasic,
                request.getHeader("Authorization"));

        // Проверяем RqUID
        assertNotNull(request.getHeader("RqUID"));

        // Проверяем тело
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("scope=GIGACHAT_API_PERS"));
    }

    @Test
    @DisplayName("500 от GigaChat → GigaChatAuthException")
    void serverError() {
        server.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        StepVerifier.create(client.requestToken())
                .expectErrorMatches(e -> e.getClass().getSimpleName().contains("AuthException"))
                .verify();
    }

    @Test
    @DisplayName("401 от GigaChat → GigaChatAuthException")
    void unauthorized() {
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("Unauthorized"));

        StepVerifier.create(client.requestToken())
                .expectError()
                .verify();
    }
}
