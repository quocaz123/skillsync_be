package com.skillsync.notification.service;

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
     * @param to           Địa chỉ email người nhận
     * @param subject      Tiêu đề email
     * @param templateName Tên file template (không có .html, ví dụ: "welcome")
     * @param variables    Các biến truyền vào template
     */
    public void sendHtmlEmail(String to, String subject, String templateName,
                               Map<String, Object> variables) {
        try {
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
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = isHtml

            mailSender.send(message);
            log.info("[EmailService] Sent '{}' email to {}", templateName, to);

        } catch (Exception e) {
            log.error("[EmailService] Failed to send '{}' email to {}: {}", templateName, to, e.getMessage(), e);
            // Không throw exception ra ngoài để tránh crash consumer
        }
    }
}
