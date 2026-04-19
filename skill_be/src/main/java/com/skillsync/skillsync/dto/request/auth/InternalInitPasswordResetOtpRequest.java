package com.skillsync.skillsync.dto.request.auth;

import lombok.Data;

@Data
public class InternalInitPasswordResetOtpRequest {
    private String email;
    private String codeHash;
    /** ISO-8601 date-time string */
    private String expiresAt;
}

