package com.skillsync.skillsync.notification.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.skillsync.entity.User;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Client gọi sang Notification-Service để gửi email (tách khỏi SkillSync backend).
 * Thất bại không được làm hỏng flow đăng nhập.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class NotificationEmailClient {

    final ObjectMapper objectMapper;

    final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${notification.base-url:http://localhost:8081/notification}")
    String notificationBaseUrl;

    public void sendWelcomeFirstLogin(User user) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) return;

        try {
            String url = notificationBaseUrl.replaceAll("/+$", "") + "/email/welcome-first-login";

            String body = objectMapper.writeValueAsString(Map.of(
                    "email", user.getEmail(),
                    "fullName", user.getFullName()
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Notification-Service welcome-first-login failed: status={}, body={}",
                        response.statusCode(), truncate(response.body()));
            }
        } catch (Exception e) {
            log.warn("Notification-Service welcome-first-login call failed: {}", e.getMessage());
        }
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() <= 600 ? s : s.substring(0, 600) + "...";
    }
}

