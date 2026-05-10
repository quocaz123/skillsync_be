package com.skillsync.skillsync.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardResponse {
    private String userId;
    private String name;
    private String avatarUrl;
    private String avatarGrad; // Dùng cho UI gradient
    private Double score;
    private String rankTier;
    private Integer position;
}
