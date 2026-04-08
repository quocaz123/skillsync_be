package com.skillsync.notification.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Health check endpoint: GET /notification/health
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
                "service", "skillsync-notification",
                "status", "UP",
                "timestamp", LocalDateTime.now().toString()
        );
    }
}
