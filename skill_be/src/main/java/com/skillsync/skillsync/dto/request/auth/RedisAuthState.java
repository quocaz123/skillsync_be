package com.skillsync.skillsync.dto.request.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Lớp chứa thông tin đăng ký tạm thời trong Redis.
 * Giúp tránh việc lưu vào Database khi chưa xác thực OTP.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisAuthState implements Serializable {
    private String email;
    private String password;    // Đã mã hóa
    private String fullName;
    private String otpCode;
    private LocalDateTime createdAt;
}
