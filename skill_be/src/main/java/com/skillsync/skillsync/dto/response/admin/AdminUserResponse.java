package com.skillsync.skillsync.dto.response.admin;

import com.skillsync.skillsync.enums.Role;
import com.skillsync.skillsync.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminUserResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private UserStatus status;
    private Role role;
    private Integer creditsBalance;
    private Integer trustScore;
    private LocalDateTime createdAt;
}
