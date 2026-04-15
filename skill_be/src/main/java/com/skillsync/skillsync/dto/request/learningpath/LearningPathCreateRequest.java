package com.skillsync.skillsync.dto.request.learningpath;

import com.skillsync.skillsync.enums.RegistrationType;
import com.skillsync.skillsync.enums.SkillCategory;
import com.skillsync.skillsync.enums.SkillLevel;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LearningPathCreateRequest {
    String title;
    String shortDescription;
    String description;
    SkillCategory category;
    SkillLevel level;
    String duration;
    String emoji;
    String thumbnailUrl;
    Integer totalCredits;
    Integer maxStudents;
    RegistrationType registrationType;
    List<LearningPathModuleRequest> modules;
}
