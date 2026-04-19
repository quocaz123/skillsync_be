package com.skillsync.skillsync.dto.request.auth;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String token;
    private String newPassword;
}

