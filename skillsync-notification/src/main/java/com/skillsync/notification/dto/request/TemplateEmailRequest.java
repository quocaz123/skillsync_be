package com.skillsync.notification.dto.request;

import java.util.Map;

/**
 * Request DTO cho email theo template (HTML).
 */
public record TemplateEmailRequest(
        String to,
        String subject,
        String templateName,
        Map<String, Object> variables
) {
}

