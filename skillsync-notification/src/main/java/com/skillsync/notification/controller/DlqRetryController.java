package com.skillsync.notification.controller;

import com.skillsync.notification.service.DlqService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notification/dlq")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Cho phép Frontend (VD: 5173) truy cập trực tiếp
public class DlqRetryController {

    private final DlqService dlqService;

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getDlqCount() {
        long count = dlqService.getDlqMessageCount();
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/retryAll")
    public ResponseEntity<Map<String, Integer>> retryDlqMessages() {
        Map<String, Integer> result = dlqService.retryDlqMessages();
        return ResponseEntity.ok(result);
    }
}
