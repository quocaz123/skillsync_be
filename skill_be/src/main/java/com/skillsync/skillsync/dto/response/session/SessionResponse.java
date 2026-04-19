package com.skillsync.skillsync.dto.response.session;

import com.skillsync.skillsync.enums.SessionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SessionResponse {
    private UUID id;
    private String videoRoomId;
    private String videoProvider;
    private SessionStatus status;
    private Integer creditCost;

    // Slot info
    private LocalDate slotDate;
    private LocalTime slotTime;
    private LocalTime slotEndTime;

    // Teacher info
    private UUID teacherId;
    private String teacherName;
    private String teacherAvatar;

    // Learner info
    private UUID learnerId;
    private String learnerName;

    // Skill info
    private UUID teachingSkillId;
    private String skillName;
    private String skillIcon;

    private String learnerNotes;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
    
    // Review info
    private Integer rating;
    private String review;
}
