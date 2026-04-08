package com.skillsync.notification.service;

import com.skillsync.notification.dto.request.TemplateEmailRequest;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Core email sending service.
 * Sử dụng Thymeleaf để render HTML template, sau đó gửi qua Gmail SMTP.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.mail.sender-email}")
    private String senderEmail;

    @Value("${app.mail.sender-name}")
    private String senderName;

    /**
     * Gửi email HTML được render từ Thymeleaf template.
     *
     * @param request Request DTO (to/subject/template/variables)
     */
    public void sendHtmlEmail(TemplateEmailRequest request) {
        try {
            if (request == null || request.to() == null || request.to().isBlank()) {
                log.warn("[EmailService] Skip sending email: missing recipient");
                return;
            }
            String templateName = request.templateName();
            Map<String, Object> variables = request.variables() != null ? request.variables() : Map.of();

            // Render Thymeleaf template → HTML string
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);

            // Tạo MimeMessage
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(senderEmail, senderName);
            helper.setTo(request.to());
            helper.setSubject(request.subject());
            helper.setText(htmlContent, true); // true = isHtml

            mailSender.send(message);
            log.info("[EmailService] Sent '{}' email to {}", templateName, request.to());

        } catch (Exception e) {
            log.error("[EmailService] Failed to send '{}' email to {}: {}", request != null ? request.templateName() : "?",
                    request != null ? request.to() : "?", e.getMessage(), e);
        }
    }
}
