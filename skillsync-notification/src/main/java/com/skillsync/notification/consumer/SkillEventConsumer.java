package com.skillsync.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.notification.dto.event.SkillEvent;
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
public class SkillEventConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @KafkaListener(
            topics = "${kafka.topics.skill}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleSkillEvent(ConsumerRecord<String, Object> record) {
        try {
            Object payload = record != null ? record.value() : null;
            SkillEvent event = objectMapper.convertValue(payload, SkillEvent.class);

            if ("SKILL_VERIFIED".equals(event.getEventType())) {
                handleSkillVerified(event);
            } else if ("SKILL_REJECTED".equals(event.getEventType())) {
                handleSkillRejected(event);
            } else {
                log.warn("[SkillEventConsumer] Unhandled eventType: {}", event.getEventType());
            }

        } catch (Exception e) {
            log.error("[SkillEventConsumer] Error processing skill event: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing skill event", e);
        }
    }

    private void handleSkillVerified(SkillEvent event) {
        Map<String, Object> variables = Map.of(
                "recipientName", event.getRecipientName() != null ? event.getRecipientName() : "bạn",
                "skillName", event.getSkillName() != null ? event.getSkillName() : "kỹ năng",
                "manageUrl", frontendUrl + "/app/teaching",
                "frontendUrl", frontendUrl
        );

        emailService.sendHtmlEmail(new TemplateEmailRequest(
                event.getRecipientEmail(),
                "✅ Kỹ năng của bạn đã được xác minh",
                "skill_verified",
                variables
        ));
    }

    private void handleSkillRejected(SkillEvent event) {
        Map<String, Object> variables = Map.of(
                "recipientName", event.getRecipientName() != null ? event.getRecipientName() : "bạn",
                "skillName", event.getSkillName() != null ? event.getSkillName() : "kỹ năng",
                "rejectionReason", event.getRejectionReason() != null ? event.getRejectionReason() : "",
                "manageUrl", frontendUrl + "/app/teaching",
                "frontendUrl", frontendUrl
        );

        emailService.sendHtmlEmail(new TemplateEmailRequest(
                event.getRecipientEmail(),
                "⚠️ Kỹ năng của bạn chưa được duyệt",
                "skill_rejected",
                variables
        ));
    }
}

