package com.skillsync.skillsync.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetOtpStateResponse {
    private String codeHash;
    private String expiresAt;
}

