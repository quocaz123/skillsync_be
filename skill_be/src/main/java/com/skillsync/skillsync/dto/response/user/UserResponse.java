package com.skillsync.skillsync.dto.response.user;

import com.skillsync.skillsync.enums.Role;
import com.skillsync.skillsync.enums.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {

    // ─── Core identity ───────────────────────────────────────
    UUID id;
    String email;
    String fullName;
    String avatarUrl;
    String bio;
    UserStatus status;
    Role role;
    Boolean hasPassword;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    // ─── Gamification ────────────────────────────────────────
    Integer creditsBalance;
    Integer pendingLearnerCredits;
    Integer pendingTeacherCredits;

    // ─── Statistics (aggregated) ─────────────────────────────
    Long totalTeachingSessions;
    Long totalLearningSessions;
    Double averageRating;
    Long totalReviews;

    // ─── Teaching skills count ───────────────────────────────
    Long totalTeachingSkills;
}
