package com.skillsync.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.notification.dto.event.SessionEvent;
import com.skillsync.notification.dto.request.TemplateEmailRequest;
import com.skillsync.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionEventConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topics.session}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void handleSessionEvent(ConsumerRecord<String, Object> record) {
        try {
            Object payload = record != null ? record.value() : null;
            SessionEvent event = objectMapper.convertValue(payload, SessionEvent.class);

            if ("BOOKING_SUCCESS".equals(event.getEventType())) {
                handleBookingSuccess(event);
            } else if ("BOOKING_CANCELLED".equals(event.getEventType())) {
                handleBookingCancelled(event);
            } else if ("BOOKING_REMINDER".equals(event.getEventType())) {
                handleBookingReminder(event);
            } else {
                log.warn("[SessionEventConsumer] Unhandled eventType: {}", event.getEventType());
            }

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
}
