package com.skillsync.skillsync.dto.request.skill;

import com.skillsync.skillsync.enums.SkillLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateTeachingSkillRequest {
    UUID skillId;
    SkillLevel level;
    String experienceDesc;
    String outcomeDesc;
    String teachingStyle;
    Integer creditsPerHour;
}
