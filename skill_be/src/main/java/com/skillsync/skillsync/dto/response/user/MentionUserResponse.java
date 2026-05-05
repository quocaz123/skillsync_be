package com.skillsync.skillsync.dto.response.user;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * DTO nhẹ — chỉ dùng cho @mention dropdown.
 * Không chứa role, credits, trustScore để tránh lộ thông tin nhạy cảm.
 */
@Data
@Builder
public class MentionUserResponse {
    private UUID id;
    private String fullName;
    private String email;      // đã được mask: a**@gmail.com
    private String avatarUrl;
}
