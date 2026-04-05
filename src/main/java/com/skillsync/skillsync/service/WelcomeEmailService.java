package com.skillsync.skillsync.service;

import com.skillsync.skillsync.entity.User;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Gửi email chào mừng khi người dùng đăng nhập thành công lần đầu.
 * Nếu chưa cấu hình {@code spring.mail.username} / mật khẩu ứng dụng Gmail thì bỏ qua, không lỗi.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class WelcomeEmailService {

    final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    String mailUsername;

    @Value("${app.mail.welcome-subject:Chào mừng bạn đến với SkillSync}")
    String welcomeSubject;

    @Async
    public void sendFirstLoginWelcome(User user) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        if (mailUsername == null || mailUsername.isBlank()) {
            log.debug("Chưa cấu hình spring.mail.username — bỏ qua email chào mừng.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailUsername);
            message.setTo(user.getEmail());
            message.setSubject(welcomeSubject);
            message.setText(buildBody(user));

            mailSender.send(message);
            log.info("Đã gửi email chào mừng lần đầu tới {}", user.getEmail());
        } catch (Exception e) {
            log.warn("Gửi email chào mừng thất bại (đăng nhập vẫn thành công): {} — {}", user.getEmail(), e.getMessage());
        }
    }

    private static String buildBody(User user) {
        String name = user.getFullName() != null && !user.getFullName().isBlank()
                ? user.getFullName()
                : "bạn";
        return "Xin chào " + name + ",\n\n"
                + "Cảm ơn bạn đã đăng nhập lần đầu vào SkillSync.\n\n"
                + "Chúc bạn học tập và chia sẻ kỹ năng hiệu quả cùng cộng đồng.\n\n"
                + "—\nSkillSync";
    }
}
