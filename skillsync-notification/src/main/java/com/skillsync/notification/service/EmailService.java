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
import java.util.Objects;

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
        trySendHtmlEmail(request);
    }

    /**
     * Same as sendHtmlEmail but returns status.
     */
    public boolean trySendHtmlEmail(TemplateEmailRequest request) {
        try {
            if (request == null || request.to() == null || request.to().isBlank()) {
                log.warn("[EmailService] Skip sending email: missing recipient");
                return false;
            }
            if (senderEmail == null || senderEmail.isBlank()) {
                log.warn("[EmailService] Skip sending email: missing sender email configuration (app.mail.sender-email)");
                return false;
            }

            String templateName = request.templateName();
            if (templateName == null || templateName.isBlank()) {
                log.warn("[EmailService] Skip sending email to {}: missing templateName", request.to());
                return false;
            }

            String subject = request.subject();
            if (subject == null || subject.isBlank()) {
                log.warn("[EmailService] Skip sending email to {}: missing subject", request.to());
                return false;
            }

            Map<String, Object> variables = request.variables() != null ? request.variables() : Map.of();

            // Render Thymeleaf template → HTML string
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = Objects.requireNonNullElse(templateEngine.process(templateName, context), "");

            final String fromEmail = Objects.requireNonNull(senderEmail, "senderEmail must not be null");
            final String to = Objects.requireNonNull(request.to(), "recipient must not be null");
            final String nonNullSubject = Objects.requireNonNull(subject, "subject must not be null");
            final String nonNullHtmlContent = Objects.requireNonNull(htmlContent, "htmlContent must not be null");

            // Tạo MimeMessage
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(nonNullSubject);
            helper.setText(nonNullHtmlContent, true); // true = isHtml

            mailSender.send(message);
            log.info("[EmailService] Sent '{}' email to {}", templateName, request.to());
            return true;

        } catch (Exception e) {
            log.error("[EmailService] Failed to send '{}' email to {}: {}", request != null ? request.templateName() : "?",
                    request != null ? request.to() : "?", e.getMessage(), e);
            return false;
        }
    }
}
