package com.eunhyehymn.application.ports;

public interface SocialTokenVerifier {
    SocialUserInfo verify(String provider, String token);
}
