package org.example.notetaskbackend.dto.gigachat_api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GigaChatAuthResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_at") Long expiresIn
) {
}