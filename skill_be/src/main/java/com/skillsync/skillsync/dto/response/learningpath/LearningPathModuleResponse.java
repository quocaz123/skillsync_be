package com.skillsync.skillsync.dto.response.learningpath;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LearningPathModuleResponse {
    String id;
    String title;
    String description;
    String objective;
    Integer orderIndex;
    Boolean enableSupport;
    Boolean hasQuiz;
    Boolean isQuizMandatory;
    List<LearningPathLessonResponse> lessons;

    Integer lessonCount;
}
