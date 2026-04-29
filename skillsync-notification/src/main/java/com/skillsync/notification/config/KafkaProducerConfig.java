package com.skillsync.notification.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Cấu hình Kafka Producer cho skillsync-notification.
 * Hỗ trợ cả local (PLAINTEXT) và Aiven Kafka (SASL_SSL + SCRAM-SHA-256).
 * SASL được bật tự động khi có env var KAFKA_SASL_USERNAME — giống hệt skill_be.
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

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

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

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

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
