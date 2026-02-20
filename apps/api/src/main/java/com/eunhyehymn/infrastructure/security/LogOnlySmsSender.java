package com.eunhyehymn.infrastructure.security;

import com.eunhyehymn.application.ports.SmsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogOnlySmsSender implements SmsSender {
    private static final Logger log = LoggerFactory.getLogger(LogOnlySmsSender.class);

    @Override
    public void sendVerificationCode(String phoneNumber, String code) {
        log.warn("SMS provider disabled. phone={} verificationCode={}", phoneNumber, code);
    }
}
