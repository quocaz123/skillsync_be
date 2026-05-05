package com.skillsync.notification.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SkillEvent {

    /** Loại sự kiện (SKILL_VERIFIED, SKILL_REJECTED) */
    String eventType;

    String recipientEmail;
    String recipientName;

    String skillName;
    String rejectionReason;

    String timestamp;
}

