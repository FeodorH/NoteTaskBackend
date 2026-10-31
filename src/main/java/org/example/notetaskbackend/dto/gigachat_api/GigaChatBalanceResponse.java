package org.example.notetaskbackend.dto.gigachat_api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GigaChatBalanceResponse(List<BalanceInfo> balance) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BalanceInfo(Long value, String type) {
    }
}
