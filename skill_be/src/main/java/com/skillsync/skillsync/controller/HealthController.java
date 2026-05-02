package com.skillsync.skillsync.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint health check đơn giản — dùng để Koyeb / load balancer ping kiểm tra app còn sống.
 * Path: GET /skillsync/health  →  200 OK {"status": "UP"}
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
