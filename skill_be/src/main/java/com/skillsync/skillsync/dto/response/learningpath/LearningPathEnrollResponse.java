package com.skillsync.skillsync.dto.response.learningpath;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningPathEnrollResponse {
    private Boolean enrolled;
    private String enrollmentId;
    private Integer creditsBalance;
    private String message;
}
