package com.skillsync.skillsync.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Origin cố định (comma-separated). Biến môi trường: CORS_ALLOWED_ORIGINS
     */
    @Value("${app.cors.allowed-origins:}")
    private String corsAllowedOrigins;

    /**
     * Pattern cho preview Pages v.v. (comma-separated). Biến môi trường: CORS_ALLOWED_ORIGIN_PATTERNS.
     * Ví dụ: https://*.skillsync.pages.dev
     */
    @Value("${app.cors.allowed-origin-patterns:}")
    private String corsAllowedOriginPatterns;

    private static final String[] DEV_FALLBACK_ORIGINS = new String[] {
            "http://localhost:3000",
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "https://skillsync-fe.pages.dev",
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/auth/register", "/auth/login", "/auth/google", "/auth/google/exchange",
                                "/auth/refresh",
                                // Chưa có JWT sau đăng ký / quên mật khẩu — phải public
                                "/auth/verify-email", "/auth/resend-verification",
                                "/auth/forgot-password", "/auth/reset-password",
                                // Health check cho Koyeb / load balancer
                                "/health")
                        .permitAll()
                        .requestMatchers("/auth/logout").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        // Learning paths public
                        .requestMatchers("/api/learning-paths/approved").permitAll()
                        .requestMatchers("/api/learning-paths/*/approve", "/api/learning-paths/*/reject").hasRole("ADMIN")
                        .requestMatchers("/auth/**").authenticated()
                        // Admin only
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Everything else requires authentication
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String[] patterns = resolveAllowedOriginPatterns();
                registry.addMapping("/**")
                        .allowedOriginPatterns(patterns)
                        .allowCredentials(true)
                        .allowedMethods("*")
                        .allowedHeaders("*")
                        .maxAge(3600);
            }
        };
    }

    private String[] resolveAllowedOriginPatterns() {
        Set<String> merged = new LinkedHashSet<>();
        if (corsAllowedOrigins == null || corsAllowedOrigins.isBlank()) {
            merged.addAll(Arrays.asList(DEV_FALLBACK_ORIGINS));
        } else {
            merged.addAll(splitCsv(corsAllowedOrigins));
        }
        merged.addAll(splitCsv(corsAllowedOriginPatterns));
        return merged.toArray(String[]::new);
    }

    private static List<String> splitCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        for (String part : raw.split(",")) {
            String s = part.trim();
            if (!s.isBlank()) {
                list.add(s);
            }
        }
        return list;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
