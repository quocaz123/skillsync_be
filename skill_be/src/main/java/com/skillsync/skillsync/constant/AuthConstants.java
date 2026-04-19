package com.skillsync.skillsync.constant;

/**
 * Hằng số xác thực — dùng chung AuthService, email Kafka, và (qua event) template notification.
 */
public final class AuthConstants {

    private AuthConstants() {}

    /** Thời gian hiệu lực mã OTP (đăng ký, gửi lại, quên mật khẩu). */
    public static final int OTP_VALID_MINUTES = 15;
}
