package com.eunhyehymn.infrastructure.security;

import com.eunhyehymn.application.ports.SmsSender;
import com.eunhyehymn.common.error.ApiException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.springframework.http.HttpStatus;

public class TwilioSmsSender implements SmsSender {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(6);

    private final HttpClient httpClient;
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final String template;

    public TwilioSmsSender(String accountSid, String authToken, String fromNumber, String template) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.template = template;
    }

    @Override
    public void sendVerificationCode(String phoneNumber, String code) {
        String message = String.format(template, code);
        String form = "To=" + encode(phoneNumber) +
            "&From=" + encode(fromNumber) +
            "&Body=" + encode(message);
        String basic = Base64.getEncoder()
            .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json"))
            .timeout(REQUEST_TIMEOUT)
            .header("Authorization", "Basic " + basic)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "sms_send_failed",
                    "SMS provider returned an error.",
                    response.statusCode()
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.BAD_GATEWAY, "sms_send_failed", "SMS request interrupted.", null);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "sms_send_failed", "SMS request failed.", null);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
