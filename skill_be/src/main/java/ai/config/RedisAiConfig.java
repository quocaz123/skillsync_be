package ai.config;

import ai.memory.AiSessionState;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration cho AI session memory.
 * <p>
 * Dùng Jackson để serialize {@link AiSessionState} thay vì Java serialization
 * → dữ liệu dễ đọc trong Redis CLI, không gặp vấn đề classpath khi deploy.
 * <p>
 * Bean name: {@code aiSessionRedisTemplate} — inject tường minh trong
 * {@link ai.memory.AiSessionRepository} để tránh conflict với
 * {@code RedisTemplate<Object, Object>} mặc định của Spring Boot.
 */
@Configuration
public class RedisAiConfig {

    @Bean
    public RedisTemplate<String, AiSessionState> aiSessionRedisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, AiSessionState> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key serializer: plain String
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value serializer: Jackson JSON
        ObjectMapper om = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Jackson2JsonRedisSerializer<AiSessionState> valueSerializer =
                new Jackson2JsonRedisSerializer<>(om, AiSessionState.class);

        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
