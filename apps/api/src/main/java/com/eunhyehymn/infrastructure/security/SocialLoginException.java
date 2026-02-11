package com.eunhyehymn.infrastructure.security;

public class SocialLoginException extends RuntimeException {
    public SocialLoginException(String message) {
        super(message);
    }
}
