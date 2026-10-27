package org.example.notetaskbackend.security;

import org.example.notetaskbackend.service.GigaChatService;
import org.example.notetaskbackend.config.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class ApiKeyAuthIntegrationTest {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String VALID_KEY = TestSecurityConfig.TEST_API_KEY;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Health actuator доступен без API-ключа")
    void healthEndpointIsPublic() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Metrics actuator требует API-ключ")
    void metricsEndpointRequiresApiKey() {
        webTestClient.get()
                .uri("/actuator/metrics")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Metrics actuator доступен с API-ключом")
    void metricsEndpointWorksWithApiKey() {
        webTestClient.get()
                .uri("/actuator/metrics")
                .header(API_KEY_HEADER, VALID_KEY)
                .exchange()
                .expectStatus().isOk();
    }
}