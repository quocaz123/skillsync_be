package com.skillsync.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.notification.dto.event.AuthEvent;
import com.skillsync.notification.dto.request.TemplateEmailRequest;
import com.skillsync.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka Consumer lắng nghe topic: skillsync.notification.auth
 * Xử lý sự kiện WELCOME — email chào mừng người dùng mới đăng ký.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @KafkaListener(topics = "${kafka.topics.auth}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void handleAuthEvent(ConsumerRecord<String, Object> record) {
        try {
            Object payload = record != null ? record.value() : null;
            AuthEvent event = objectMapper.convertValue(payload, AuthEvent.class);

            if ("WELCOME".equals(event.getEventType())) {
                handleWelcome(event);
            } else {
                log.warn("[AuthEventConsumer] Unhandled eventType: {}", event.getEventType());
            }

        } catch (Exception e) {
            log.error("[AuthEventConsumer] Error processing auth event: {}", e.getMessage(), e);
        }
    }

    // ── WELCOME ──────────────────────────────────────────────────────────────

    private void handleWelcome(AuthEvent event) {
        Map<String, Object> variables = Map.of(
                "recipientName", event.getRecipientName() != null ? event.getRecipientName() : "bạn",
                "recipientEmail", event.getRecipientEmail(),
                "exploreUrl", frontendUrl + "/explore",
                "profileUrl", frontendUrl + "/app/profile",
                "frontendUrl", frontendUrl);

        emailService.sendHtmlEmail(new TemplateEmailRequest(
                event.getRecipientEmail(),
                "🎉 Chào mừng bạn đến với SkillSync!",
                "welcome",
                variables));

    }
}
