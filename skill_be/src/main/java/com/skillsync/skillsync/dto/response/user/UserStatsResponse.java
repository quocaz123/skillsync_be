package com.skillsync.skillsync.dto.response.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserStatsResponse {
    private Integer creditsBalance;
    private Long teachingSessionsThisMonth;
    private Long learningSessionsThisMonth;
    private Double averageRating;
}
