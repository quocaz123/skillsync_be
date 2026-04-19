package ai.dto.response;

import java.util.UUID;
public record MentorMatchDto (
    UUID mentorId,
    String fullName,
    String avatarUrl,
    String skillName,
    String level,
    String experienceDesc,
    String teachingStyle,
    Integer creditsPerHour,
    Double rating,
    String aiReason
) {}
