package com.skillsync.skillsync.dto.response.learningpath;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LearningPathReviewResponse {
    UUID id;
    UUID learningPathId;
    UUID reviewerId;
    String reviewerName;
    String reviewerAvatarUrl;
    Integer rating;
    String comment;
    List<String> tags;
    LocalDateTime createdAt;
}
