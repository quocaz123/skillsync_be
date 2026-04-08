package com.skillsync.skillsync.dto.response.review;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReviewResponse {
    private UUID id;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    
    // Reviewer info
    private UUID reviewerId;
    private String reviewerName;
    private String reviewerAvatar;
    
    // Session/Skill info
    private UUID sessionId;
    private String skillName;
}
