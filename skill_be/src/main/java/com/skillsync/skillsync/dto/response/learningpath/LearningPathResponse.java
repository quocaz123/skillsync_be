package com.skillsync.skillsync.dto.response.learningpath;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LearningPathResponse {
    String id;
    String title;
    String shortDescription;
    String description;
    String category;
    String level;
    String duration;
    String emoji;
    String thumbnailUrl;
    Integer totalCredits;
    Integer maxStudents;
    String registrationType;
    String status;
    String rejectionReason;

    // Teacher info
    String teacherId;
    String teacherName;
    String teacherAvatarUrl;
    String teacherRole;
    String teacherBio;

    // Stats
    Integer moduleCount;
    Integer lessonCount;
    Integer enrollmentCount;
    Integer progressPercent;
    String enrollmentStatus;
    Double rating;
    Integer totalReviews;

    List<LearningPathModuleResponse> modules;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
