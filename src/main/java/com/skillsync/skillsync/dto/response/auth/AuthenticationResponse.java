package com.skillsync.skillsync.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticationResponse {
    private String accessToken;
    private String refreshToken;
    private String userId;
    private String email;
    private String fullName;
    private String role;
    private String avatarUrl;
    private Boolean hasPassword;
    private Integer creditsBalance;
    /** true khi đây là lần đăng nhập thành công đầu tiên (email/Google), không áp dụng cho refresh token. */
    private Boolean isFirstLogin;
}
