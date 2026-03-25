package com.skillsync.skillsync.dto.request.skill;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerifyTeachingSkillRequest {
    /** APPROVED hoặc REJECTED */
    String action;
    /** Bắt buộc khi action = REJECTED */
    String rejectionReason;
}
