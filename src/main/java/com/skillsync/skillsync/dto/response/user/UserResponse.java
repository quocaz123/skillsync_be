package com.skillsync.skillsync.dto.response;

import com.skillsync.skillsync.enums.Role;
import com.skillsync.skillsync.enums.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    UUID id;
    String email;
    String fullName;
    String avatarUrl;
    UserStatus status;
    Role role;
    Integer creditsBalance;
    Integer trustScore;
    String bio;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
