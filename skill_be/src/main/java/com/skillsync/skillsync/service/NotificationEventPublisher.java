package com.skillsync.skillsync.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service publish notification events lên Kafka.
 *
 * <p>Được gọi từ các business service (AuthService, SessionService, ...)
 * ngay sau khi gọi notificationService.createAndSend() (in-app notification).
 * skillsync-notification service sẽ consume và gửi email tương ứng.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.auth}")
    private String authTopic;

    @Value("${kafka.topics.session}")
    private String sessionTopic;

    @Value("${kafka.topics.skill}")
    private String skillTopic;

    @Value("${kafka.topics.credit}")
    private String creditTopic;

    // ── AUTH Events ──────────────────────────────────────────────────────────

    /**
     * Publish WELCOME event khi người dùng đăng ký mới thành công.
     *
     * @param email    Email người dùng mới
     * @param fullName Tên đầy đủ
     */
    public void publishWelcome(String email, String fullName) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "WELCOME");
        event.put("recipientEmail", email);
        event.put("recipientName", fullName);
        event.put("timestamp", LocalDateTime.now().toString());

        sendSafely(authTopic, email, event, "WELCOME");
    }

    /**
     * Publish PASSWORD_RESET event when user requests a password reset.
     */
    public void publishPasswordReset(String email, String fullName, String resetUrl) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "PASSWORD_RESET");
        event.put("recipientEmail", email);
        event.put("recipientName", fullName);
        event.put("resetUrl", resetUrl);
        event.put("timestamp", LocalDateTime.now().toString());

        sendSafely(authTopic, email, event, "PASSWORD_RESET");
    }

    /**
     * Request email verification OTP to be generated/sent by skillsync-notification.
     */
    public void publishEmailVerificationRequest(String email, String fullName) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "EMAIL_VERIFICATION_REQUEST");
        event.put("recipientEmail", email);
        event.put("recipientName", fullName);
        event.put("timestamp", LocalDateTime.now().toString());

        sendSafely(authTopic, email, event, "EMAIL_VERIFICATION_REQUEST");
    }

    // ── SESSION Events ────────────────────────────────────────────────────────

    /**
     * Publish SESSION event khi có thay đổi trạng thái session.
     *
     * @param eventType       Loại event (SESSION_BOOKED, SESSION_APPROVED, ...)
     * @param recipientEmail  Email người nhận
     * @param recipientName   Tên người nhận
     * @param senderName      Tên người gửi (mentor hoặc learner)
     * @param skillName       Tên kỹ năng
     * @param slotDate        Ngày học (định dạng chuỗi)
     * @param slotTime        Giờ học (định dạng chuỗi)
     * @param creditCost      Số credits của session
     * @param sessionId       ID session (dùng trong redirect URL)
     */
    public void publishSessionEvent(String eventType,
                                    String recipientEmail, String recipientName,
                                    String senderName, String skillName,
                                    String slotDate, String slotTime,
                                    Integer creditCost, String sessionId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("recipientEmail", recipientEmail);
        event.put("recipientName", recipientName);
        event.put("senderName", senderName);
        event.put("skillName", skillName);
        event.put("slotDate", slotDate);
        event.put("slotTime", slotTime);
        event.put("creditCost", creditCost);
        event.put("sessionId", sessionId);
        event.put("timestamp", LocalDateTime.now().toString());

        sendSafely(sessionTopic, recipientEmail, event, eventType);
    }

    // ── SKILL Events ──────────────────────────────────────────────────────────

    /**
     * Publish SKILL_VERIFIED hoặc SKILL_REJECTED event.
     */
    public void publishSkillEvent(String eventType,
                                   String recipientEmail, String recipientName,
                                   String skillName, String rejectionReason) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("recipientEmail", recipientEmail);
        event.put("recipientName", recipientName);
        event.put("skillName", skillName);
        event.put("rejectionReason", rejectionReason);
        event.put("timestamp", LocalDateTime.now().toString());

        sendSafely(skillTopic, recipientEmail, event, eventType);
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void sendSafely(String topic, String key, Object payload, String eventType) {
        try {
            kafkaTemplate.send(topic, key, payload);
            log.info("[NotificationEventPublisher] Published '{}' event to topic '{}'", eventType, topic);
        } catch (Exception e) {
            // Không fail business logic nếu Kafka lỗi — chỉ log
            log.error("[NotificationEventPublisher] Failed to publish '{}' event to topic '{}': {}",
                    eventType, topic, e.getMessage());
        }
    }
}
