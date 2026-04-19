package com.skillsync.notification.controller;

import com.skillsync.notification.dto.request.TemplateEmailRequest;
import com.skillsync.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/mail")
@RequiredArgsConstructor
public class MailTestController {

    private final EmailService emailService;

    @PostMapping("/test")
    public Map<String, Object> sendTest(@RequestBody Map<String, String> body) {
        String to = body != null ? body.get("to") : null;
        boolean ok = emailService.trySendHtmlEmail(new TemplateEmailRequest(
                to,
                "SkillSync SMTP test",
                "test",
                Map.of("timestamp", LocalDateTime.now().toString())
        ));
        return Map.of("ok", ok);
    }
}

