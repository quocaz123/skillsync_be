package com.skillsync.skillsync.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationStateResponse {
    private Boolean emailVerified;
    private String codeHash;
    private String expiresAt;
}

