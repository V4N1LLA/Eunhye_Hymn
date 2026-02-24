package com.eunhyehymn.application.ports;

public interface SmsSender {
    void sendVerificationCode(String phoneNumber, String code);
}
