package com.skillsync.skillsync.dto.request.learningpath;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LearningPathLessonRequest {
    String title;
    String description;
    String videoUrl;
    Integer durationMinutes;
    Boolean isPreview;
}
