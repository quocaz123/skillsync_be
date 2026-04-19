package com.skillsync.skillsync.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.skillsync.skillsync.dto.request.auth.RedisAuthState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Cấu hình RedisTemplate cho việc lưu trữ trạng thái đăng ký tạm thời.
 */
@Configuration
public class RedisAuthConfig {

    @Bean
    public RedisTemplate<String, RedisAuthState> redisAuthStateTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, RedisAuthState> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key dùng String
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value dùng JSON (Jackson)
        ObjectMapper om = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Jackson2JsonRedisSerializer<RedisAuthState> valueSerializer =
                new Jackson2JsonRedisSerializer<>(om, RedisAuthState.class);

        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
