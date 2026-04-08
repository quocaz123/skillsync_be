package com.skillsync.skillsync.dto.response.mission;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserMissionResponse {
    private String id;
    private String title;
    private String description;
    private Integer reward;
    private String type;
    private String status;
    private String targetAction;
}
