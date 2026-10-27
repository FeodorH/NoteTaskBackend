package org.example.notetaskbackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class GigaChatUnavailableException extends RuntimeException {
    public GigaChatUnavailableException(String message) { super(message); }
    public GigaChatUnavailableException(String message, Throwable cause) { super(message, cause); }
}