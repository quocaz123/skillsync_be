package com.skillsync.skillsync.dto.response.skill;

import com.skillsync.skillsync.enums.SkillCategory;
import com.skillsync.skillsync.enums.SkillLevel;
import com.skillsync.skillsync.enums.VerificationStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeachingSkillResponse {
    UUID id;
    UUID skillId;
    String skillName;
    String skillIcon;
    SkillCategory skillCategory;
    SkillLevel level;
    String experienceDesc;
    String outcomeDesc;
    Integer creditsPerHour;
    VerificationStatus verificationStatus;
    LocalDateTime createdAt;
}
