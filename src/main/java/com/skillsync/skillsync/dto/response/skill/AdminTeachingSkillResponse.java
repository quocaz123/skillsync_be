package com.skillsync.skillsync.dto.response.skill;

import com.skillsync.skillsync.enums.SkillCategory;
import com.skillsync.skillsync.enums.SkillLevel;
import com.skillsync.skillsync.enums.VerificationStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response trả về cho Admin — đầy đủ hơn TeachingSkillResponse thông thường.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminTeachingSkillResponse {
    UUID id;

    // User info
    UUID userId;
    String userEmail;
    String userFullName;
    String userAvatarUrl;

    // Skill info
    UUID skillId;
    String skillName;
    String skillIcon;
    SkillCategory skillCategory;

    // Teaching skill details
    SkillLevel level;
    String experienceDesc;
    String outcomeDesc;
    Integer creditsPerHour;

    // Verification
    VerificationStatus verificationStatus;
    String rejectionReason;
    LocalDateTime verifiedAt;
    String verifiedByEmail;

    // Evidences
    List<EvidenceSummary> evidences;

    LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenceSummary {
        UUID id;
        String evidenceType;
        String title;
        String fileUrl;
        String externalUrl;
        Boolean isVerified;
    }
}
