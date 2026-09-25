package org.example.notetaskbackend.exception_handler;

import lombok.extern.slf4j.Slf4j;
import org.example.notetaskbackend.exception.GigaChatAuthException;
import org.example.notetaskbackend.exception.GigaChatUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(WebExchangeBindException e) {
        String details = e.getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", details);
        return ResponseEntity.badRequest().body(Map.of(
                "status", "validation_error",
                "message", details
        ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatus(ResponseStatusException e) {
        String details = e.getMessage();
        log.error("Internal server error: "+details);
        return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                "status", "error",
                "message", e.getReason() == null ? "Unknown error" : e.getReason()
        ));
    }

    @ExceptionHandler(GigaChatUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleUnavailable(GigaChatUnavailableException e) {
        log.error("GigaChat unavailable: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", "gigachat_unavailable",
                "message", "GigaChat временно недоступен"
        ));
    }

    @ExceptionHandler(GigaChatAuthException.class)
    public ResponseEntity<Map<String, String>> handleAuth(GigaChatAuthException e) {
        log.error("GigaChat auth failed: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "status", "gigachat_auth_error",
                "message", "Ошибка авторизации в GigaChat"
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Internal server error"
        ));
    }
}
