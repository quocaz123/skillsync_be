package com.skillsync.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.notification.dto.event.SessionEvent;
import com.skillsync.notification.dto.request.TemplateEmailRequest;
import com.skillsync.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionEventConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @KafkaListener(topics = "${kafka.topics.session}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void handleSessionEvent(ConsumerRecord<String, Object> record) {
        try {
            Object payload = record != null ? record.value() : null;
            SessionEvent event = objectMapper.convertValue(payload, SessionEvent.class);

            String type = event.getEventType();
            if ("BOOKING_SUCCESS".equals(type)) {
                handleBookingSuccess(event);
                return;
            }
            if ("BOOKING_CANCELLED".equals(type)) {
                handleBookingCancelled(event);
                return;
            }
            if ("BOOKING_REMINDER".equals(type)) {
                handleBookingReminder(event);
                return;
            }

            // ── Domain events từ skill_be ──────────────────────────────────
            if ("SESSION_BOOKED".equals(type)) {
                handleSessionBooked(event);
                return;
            }
            if ("SESSION_APPROVED".equals(type)) {
                handleSessionApproved(event);
                return;
            }
            if ("SESSION_REJECTED".equals(type)) {
                handleSessionRejected(event);
                return;
            }
            if ("SESSION_CANCELLED".equals(type)) {
                handleSessionCancelled(event);
                return;
            }

            log.warn("[SessionEventConsumer] Unhandled eventType: {}", type);

        } catch (Exception e) {
            log.error("[SessionEventConsumer] Error processing session event: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing session event", e); // Re-throw for DLQ Retry
        }
    }

    private void handleBookingSuccess(SessionEvent event) {
        Map<String, Object> variables = Map.of(
                "recipientName", event.getRecipientName() != null ? event.getRecipientName() : "bạn",
                "counterpartName", event.getCounterpartName() != null ? event.getCounterpartName() : "đối tác",
                "sessionTime", event.getSessionTime() != null ? event.getSessionTime() : "không xác định",
                "sessionLink", event.getSessionLink() != null ? event.getSessionLink() : "#"
        );

        emailService.sendHtmlEmail(new TemplateEmailRequest(
                event.getRecipientEmail(),
                "✅ Đặt lịch thành công",
                "booking_success",
                variables));
    }

    private void handleBookingCancelled(SessionEvent event) {
        Map<String, Object> variables = Map.of(
                "recipientName", event.getRecipientName() != null ? event.getRecipientName() : "bạn",
                "counterpartName", event.getCounterpartName() != null ? event.getCounterpartName() : "đối tác",
                "sessionTime", event.getSessionTime() != null ? event.getSessionTime() : "không xác định"
        );

        emailService.sendHtmlEmail(new TemplateEmailRequest(
                event.getRecipientEmail(),
                "🚫 Phiên học đã bị hủy",
                "booking_cancelled",
                variables));
    }

    private void handleBookingReminder(SessionEvent event) {
        Map<String, Object> variables = Map.of(
                "recipientName", event.getRecipientName() != null ? event.getRecipientName() : "bạn",
                "counterpartName", event.getCounterpartName() != null ? event.getCounterpartName() : "đối tác",
                "sessionTime", event.getSessionTime() != null ? event.getSessionTime() : "không xác định",
                "sessionLink", event.getSessionLink() != null ? event.getSessionLink() : "#"
        );

        emailService.sendHtmlEmail(new TemplateEmailRequest(
                event.getRecipientEmail(),
                "⏰ Nhắc nhở phiên học sắp diễn ra",
                "booking_reminder",
                variables));
    }

    // ── Domain handlers (SESSION_*) ───────────────────────────────────────

    private void handleSessionBooked(SessionEvent event) {
        String sessionTime = formatSessionTime(event);
        String sessionLink = frontendUrl + "/app/teaching";

        Map<String, Object> variables = Map.of(
                "recipientName", safeRecipient(event.getRecipientName()),
                "counterpartName", safe(event.getSenderName()),
                "sessionTime", sessionTime,
                "sessionLink", sessionLink
        );

        emailService.sendHtmlEmail(new TemplateEmailRequest(
                event.getRecipientEmail(),
                "📌 Có yêu cầu đặt lịch mới",
                "booking_success",
                variables
        ));
    }

    private void handleSessionApproved(SessionEvent event) {
        String sessionTime = formatSessionTime(event);
        String sessionLink = frontendUrl + "/app/sessions";

        Map<String, Object> variables = Map.of(
                "recipientName", safeRecipient(event.getRecipientName()),
                "counterpartName", safe(event.getSenderName()),
                "sessionTime", sessionTime,
                "sessionLink", sessionLink
        );

        emailService.sendHtmlEmail(new TemplateEmailRequest(
                event.getRecipientEmail(),
                "✅ Lịch học đã được chấp nhận",
                "booking_success",
                variables
        ));
    }

    private void handleSessionRejected(SessionEvent event) {
        String sessionTime = formatSessionTime(event);

        Map<String, Object> variables = Map.of(
                "recipientName", safeRecipient(event.getRecipientName()),
                "counterpartName", safe(event.getSenderName()),
                "sessionTime", sessionTime
        );

        emailService.sendHtmlEmail(new TemplateEmailRequest(
                event.getRecipientEmail(),
                "🚫 Lịch học đã bị từ chối",
                "booking_cancelled",
                variables
        ));
    }

    private void handleSessionCancelled(SessionEvent event) {
        String sessionTime = formatSessionTime(event);

        Map<String, Object> variables = Map.of(
                "recipientName", safeRecipient(event.getRecipientName()),
                "counterpartName", safe(event.getSenderName()),
                "sessionTime", sessionTime
        );

        emailService.sendHtmlEmail(new TemplateEmailRequest(
                event.getRecipientEmail(),
                "🚫 Lịch học đã bị hủy",
                "booking_cancelled",
                variables
        ));
    }

    private String formatSessionTime(SessionEvent event) {
        String d = event.getSlotDate();
        String t = event.getSlotTime();
        if (d == null && t == null) return "không xác định";
        if (d == null) return t;
        if (t == null) return d;
        return d + " " + t;
    }

    private static String safe(String v) {
        return (v != null && !v.isBlank()) ? v : "đối tác";
    }

    private static String safeRecipient(String v) {
        return (v != null && !v.isBlank()) ? v : "bạn";
    }
}
