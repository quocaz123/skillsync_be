package com.skillsync.notification.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO truyền qua Kafka topic: skillsync.notification.auth
 * Dùng cho sự kiện WELCOME — chào mừng người dùng mới đăng ký.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthEvent {

    /** Loại sự kiện (hiện tại: WELCOME) */
    String eventType;

    /** Email người nhận */
    String recipientEmail;

    /** Tên đầy đủ của người nhận */
    String recipientName;

    /** Password reset URL (for PASSWORD_RESET event) */
    String resetUrl;

    /** 6-digit verification code (for EMAIL_VERIFICATION event) */
    String verificationCode;

    /** Thời điểm sự kiện xảy ra (ISO-8601 string) */
    String timestamp;
}
