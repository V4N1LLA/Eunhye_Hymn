package com.eunhyehymn.application.ports;

public class ExternalAiException extends RuntimeException {
    public ExternalAiException(String message) {
        super(message);
    }

    public ExternalAiException(String message, Throwable cause) {
        super(message, cause);
    }
}
