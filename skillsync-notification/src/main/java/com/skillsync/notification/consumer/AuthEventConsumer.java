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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
            } else if ("VERIFY_ACCOUNT".equals(event.getEventType())) {
                handleVerifyAccount(event);
            } else if ("RESET_PASSWORD".equals(event.getEventType()) || "FORGOT_PASSWORD".equals(event.getEventType())) {
                handleResetPasswordEmail(event);
            } else {
                log.warn("[AuthEventConsumer] Unhandled eventType: {}", event.getEventType());
            }

        } catch (Exception e) {
            log.error("[AuthEventConsumer] Error processing auth event: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing auth event", e); // Re-throw to trigger retry
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

    private void handleVerifyAccount(AuthEvent event) {
        Map<String, Object> variables = Map.of(
                "recipientName", event.getRecipientName() != null ? event.getRecipientName() : "bạn",
                "otpCode", event.getOtpCode() != null ? event.getOtpCode() : "N/A",
                "frontendUrl", frontendUrl);

        emailService.sendHtmlEmail(new TemplateEmailRequest(
                event.getRecipientEmail(),
                "Xác minh tài khoản SkillSync của bạn",
                "verify_email",
                variables));
    }

    /** OTP đặt lại / thiết lập mật khẩu (cùng template reset_password). */
    private void handleResetPasswordEmail(AuthEvent event) {
        String email = event.getRecipientEmail();
        String resetUrl = frontendUrl + "/reset-password?email=" + URLEncoder.encode(email != null ? email : "", StandardCharsets.UTF_8);
        Map<String, Object> variables = Map.of(
                "recipientName", event.getRecipientName() != null ? event.getRecipientName() : "bạn",
                "otpCode", event.getOtpCode() != null ? event.getOtpCode() : "N/A",
                "frontendUrl", frontendUrl,
                "resetUrl", resetUrl);

        emailService.sendHtmlEmail(new TemplateEmailRequest(
                email,
                "Thiết lập / đặt lại mật khẩu SkillSync",
                "reset_password",
                variables));
    }
}
