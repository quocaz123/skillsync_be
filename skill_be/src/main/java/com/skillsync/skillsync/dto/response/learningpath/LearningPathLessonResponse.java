package com.skillsync.skillsync.dto.response.learningpath;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LearningPathLessonResponse {
    String id;
    String title;
    String description;
    String videoUrl;
    Integer durationMinutes;
    Boolean isPreview;
    Integer orderIndex;
}
