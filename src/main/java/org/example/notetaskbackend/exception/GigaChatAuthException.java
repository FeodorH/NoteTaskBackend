package org.example.notetaskbackend.exception;

public class GigaChatAuthException extends RuntimeException {
    public GigaChatAuthException(String message) { super(message); }
    public GigaChatAuthException(String message, Throwable cause) { super(message, cause); }
}