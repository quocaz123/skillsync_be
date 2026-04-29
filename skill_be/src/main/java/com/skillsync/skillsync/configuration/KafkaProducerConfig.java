package com.skillsync.skillsync.configuration;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Cấu hình Kafka Producer cho skill_be.
 * Hỗ trợ cả local (plain) và Upstash Kafka (SASL_SSL).
 * SASL được bật tự động khi có env var KAFKA_SASL_USERNAME.
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /** Upstash REST username — rỗng khi dùng local Kafka */
    @Value("${spring.kafka.properties.sasl-username:}")
    private String saslUsername;

    /** Upstash REST password — rỗng khi dùng local Kafka */
    @Value("${spring.kafka.properties.sasl-password:}")
    private String saslPassword;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Không gắn type header để consumer tự deserialize
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        // Bật SASL_SSL nếu có credential (Upstash Kafka)
        if (saslUsername != null && !saslUsername.isBlank()) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
            props.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-256");
            props.put(SaslConfigs.SASL_JAAS_CONFIG,
                    "org.apache.kafka.common.security.scram.ScramLoginModule required " +
                    "username=\"" + saslUsername + "\" " +
                    "password=\"" + saslPassword + "\";");
        }

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
