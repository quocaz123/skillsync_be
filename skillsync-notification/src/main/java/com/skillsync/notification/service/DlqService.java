package com.skillsync.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DlqService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ConsumerFactory<String, Object> consumerFactory;

    @Value("${kafka.topics.dlq}")
    private String dlqTopic;

    public long getDlqMessageCount() {
        long totalLag = 0;
        try (Consumer<String, Object> consumer = consumerFactory.createConsumer("dlq-admin-group", "")) {
            java.util.List<org.apache.kafka.common.PartitionInfo> partitionInfos = consumer.partitionsFor(dlqTopic);
            if (partitionInfos == null) return 0;

            java.util.List<org.apache.kafka.common.TopicPartition> partitions = partitionInfos.stream()
                    .map(info -> new org.apache.kafka.common.TopicPartition(info.topic(), info.partition()))
                    .collect(java.util.stream.Collectors.toList());

            Map<org.apache.kafka.common.TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);
            Map<org.apache.kafka.common.TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> committedOffsets = 
                    consumer.committed(new java.util.HashSet<>(partitions));

            for (org.apache.kafka.common.TopicPartition partition : partitions) {
                long endOffset = endOffsets.getOrDefault(partition, 0L);
                org.apache.kafka.clients.consumer.OffsetAndMetadata metadata = committedOffsets.get(partition);
                
                long committedOffset = 0L;
                if (metadata != null) {
                    committedOffset = metadata.offset();
                } else {
                    Map<org.apache.kafka.common.TopicPartition, Long> startOffsets = 
                            consumer.beginningOffsets(Collections.singletonList(partition));
                    committedOffset = startOffsets.getOrDefault(partition, 0L);
                }
                
                long lag = endOffset - committedOffset;
                if (lag > 0) totalLag += lag;
            }
        } catch (Exception e) {
            log.error("[DlqService] Error calculating DLQ count: ", e);
        }
        return totalLag;
    }

    /**
     * Polls the DLQ topic for messages and republishes them to their original topic.
     * Returns the count of messages that were retried.
     */
    public Map<String, Integer> retryDlqMessages() {
        int count = 0;
        try (Consumer<String, Object> consumer = consumerFactory.createConsumer("dlq-admin-group", "")) {
            consumer.subscribe(Collections.singletonList(dlqTopic));
            
            // Poll for messages wait up to 5 seconds
            ConsumerRecords<String, Object> records = consumer.poll(Duration.ofSeconds(5));
            
            if (records.isEmpty()) {
                log.info("[DlqService] No messages found in DLQ.");
                return Map.of("retried", 0);
            }

            for (ConsumerRecord<String, Object> record : records) {
                String originalTopic = null;
                if (record.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC) != null) {
                    originalTopic = new String(record.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC).value(), StandardCharsets.UTF_8);
                }
                
                if (originalTopic != null) {
                    kafkaTemplate.send(originalTopic, record.key(), record.value());
                    log.info("[DlqService] Republishing message from DLQ to original topic: {}", originalTopic);
                    count++;
                } else {
                    log.warn("[DlqService] Message in DLQ missing original topic header. Skipping.");
                }
            }
            // Commit offsets so we don't read them again next time
            consumer.commitSync();
            log.info("[DlqService] Successfully retried {} messages from DLQ.", count);
            
        } catch (Exception e) {
            log.error("[DlqService] Error while processing DLQ: ", e);
            throw new RuntimeException("Error processing DLQ", e);
        }
        
        Map<String, Integer> result = new HashMap<>();
        result.put("retried", count);
        return result;
    }
}
