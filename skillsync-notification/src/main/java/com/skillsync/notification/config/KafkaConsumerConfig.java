package com.skillsync.notification.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Cấu hình Kafka Consumer cho skillsync-notification.
 * Hỗ trợ cả local (PLAINTEXT) và Aiven Kafka (SASL_SSL + SCRAM-SHA-256).
 * SASL được bật tự động khi có env var KAFKA_SASL_USERNAME — giống hệt skill_be.
 */
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /** Aiven username — rỗng khi dùng local Kafka */
    @Value("${spring.kafka.properties.sasl-username:}")
    private String saslUsername;

    /** Aiven password — rỗng khi dùng local Kafka */
    @Value("${spring.kafka.properties.sasl-password:}")
    private String saslPassword;

    /**
     * Đường dẫn đến file CA certificate (PEM) của Aiven.
     * Đặt biến môi trường KAFKA_SSL_CA_CERT_PATH khi chạy local Windows.
     * Koyeb/Linux: để rỗng — hệ thống đã tin tưởng CA này.
     */
    @Value("${spring.kafka.properties.ssl-ca-cert-path:}")
    private String sslCaCertPath;

    @Value("${kafka.topics.dlq}")
    private String dlqTopic;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        JsonDeserializer<Object> deserializer = new JsonDeserializer<>(Object.class);
        deserializer.addTrustedPackages("com.skillsync.notification.dto.event",
                                        "com.skillsync.skillsync.*");
        deserializer.setUseTypeHeaders(false);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 900000);

        // Bật SASL_SSL nếu có credential (Aiven Kafka) — giống KafkaProducerConfig của skill_be
        if (saslUsername != null && !saslUsername.isBlank()) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
            props.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-256");
            props.put(SaslConfigs.SASL_JAAS_CONFIG,
                    "org.apache.kafka.common.security.scram.ScramLoginModule required " +
                    "username=\"" + saslUsername + "\" " +
                    "password=\"" + saslPassword + "\";");

            // Cấu hình SSL truststore dạng PEM (Aiven CA cert)
            // Koyeb/Linux: hệ thống đã trust → sslCaCertPath rỗng → bỏ qua
            // Windows local: set KAFKA_SSL_CA_CERT_PATH = đường dẫn file ca.pem
            if (sslCaCertPath != null && !sslCaCertPath.isBlank()) {
                try {
                    String caCert = Files.readString(Paths.get(sslCaCertPath));
                    props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");
                    props.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, caCert);
                } catch (IOException e) {
                    throw new RuntimeException(
                        "Không thể đọc Aiven CA cert tại: " + sslCaCertPath, e);
                }
            }
        }

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public DefaultErrorHandler errorHandler(org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (r, e) -> new org.apache.kafka.common.TopicPartition(dlqTopic, r.partition())
            );
        FixedBackOff backOff = new FixedBackOff(120000L, 5L);
        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
