package com.skillsync.skillsync.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthResponse {
    private String userId;
    private String email;
    private String fullName;
    private String role;
    private String avatarUrl;
    private Boolean hasPassword;
    private Integer creditsBalance;
}
