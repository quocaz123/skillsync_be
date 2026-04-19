package com.skillsync.skillsync.dto.request.learningpath;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LearningPathModuleRequest {
    String title;
    String description;
    String objective;
    Boolean enableSupport;
    Boolean hasQuiz;
    Boolean isQuizMandatory;
    Integer sessionsNeeded;
    List<LearningPathLessonRequest> lessons;

}
