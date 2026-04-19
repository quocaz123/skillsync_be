package com.skillsync.skillsync.dto.response.skill;

import com.skillsync.skillsync.enums.SkillCategory;
import com.skillsync.skillsync.enums.SkillLevel;
import com.skillsync.skillsync.enums.VerificationStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import com.skillsync.skillsync.dto.response.review.ReviewResponse;
import java.time.LocalDateTime;
import java.util.List;
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
    String teachingStyle;
    Integer creditsPerHour;
    VerificationStatus verificationStatus;
    String rejectionReason;
    /** true = mentor tạm ẩn — không Explore/AI, không nhận booking mới */
    Boolean hidden;
    
    Long openSlotsCount;
    Long totalSessions;
    
    
    // Teacher details for explore page
    UUID teacherId;
    String teacherName;
    String teacherAvatar;
    String teacherBio;
    
    List<EvidenceResponse> evidences;
    List<ReviewResponse> reviews;
    
    LocalDateTime createdAt;
}
