package org.example.notetaskbackend.config;

import io.netty.handler.ssl.SslContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class GigaChatWebClients {

    /** Клиент для GigaChat */
    @Bean
    public WebClient gigaChatWebClient(GigaChatProperties props, SslContext sslContext) {
        HttpClient httpClient = HttpClient.create()
                .secure(spec -> spec.sslContext(sslContext))
                .responseTimeout(Duration.ofSeconds(60));

        return WebClient.builder()
                .baseUrl(props.apiUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /** Клиент для авторизации GigaChat */
    @Bean
    public WebClient gigaChatAuthWebClient(GigaChatProperties props, SslContext sslContext) {
        HttpClient httpClient = HttpClient.create()
                .secure(spec -> spec.sslContext(sslContext))
                .responseTimeout(Duration.ofSeconds(30));

        return WebClient.builder()
                .baseUrl(props.authUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
