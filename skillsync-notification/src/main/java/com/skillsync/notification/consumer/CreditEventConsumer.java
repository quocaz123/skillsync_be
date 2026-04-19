package com.skillsync.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.notification.dto.event.CreditEvent;
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
public class CreditEventConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topics.credit}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void handleCreditEvent(ConsumerRecord<String, Object> record) {
        try {
            Object payload = record != null ? record.value() : null;
            CreditEvent event = objectMapper.convertValue(payload, CreditEvent.class);

            if ("DEPOSIT_SUCCESS".equals(event.getEventType())) {
                handleDepositSuccess(event);
            } else if ("WITHDRAWAL_SUCCESS".equals(event.getEventType())) {
                handleWithdrawalSuccess(event);
            } else {
                log.warn("[CreditEventConsumer] Unhandled eventType: {}", event.getEventType());
            }

        } catch (Exception e) {
            log.error("[CreditEventConsumer] Error processing credit event: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing credit event", e); // Re-throw for DLQ Retry
        }
    }

    private void handleDepositSuccess(CreditEvent event) {
        Map<String, Object> variables = Map.of(
                "recipientName", event.getRecipientName() != null ? event.getRecipientName() : "bạn",
                "amount", event.getAmount() != null ? event.getAmount() : "0",
                "balance", event.getBalance() != null ? event.getBalance() : "0"
        );

        emailService.sendHtmlEmail(new TemplateEmailRequest(
                event.getRecipientEmail(),
                "💰 Nạp tiền thành công",
                "deposit_success",
                variables));
    }

    private void handleWithdrawalSuccess(CreditEvent event) {
        Map<String, Object> variables = Map.of(
                "recipientName", event.getRecipientName() != null ? event.getRecipientName() : "bạn",
                "amount", event.getAmount() != null ? event.getAmount() : "0",
                "balance", event.getBalance() != null ? event.getBalance() : "0"
        );

        emailService.sendHtmlEmail(new TemplateEmailRequest(
                event.getRecipientEmail(),
                "💸 Rút tiền thành công",
                "withdrawal_success",
                variables));
    }
}
