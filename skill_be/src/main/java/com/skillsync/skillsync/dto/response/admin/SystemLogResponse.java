package com.skillsync.skillsync.dto.response.admin;

import com.skillsync.skillsync.enums.LogLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SystemLogResponse {
    private UUID id;
    private String action;
    private LogLevel level;
    private String ipAddress;
    private LocalDateTime createdAt;
    
    private UUID userId;
    private String userEmail;
    private String userName;
}
