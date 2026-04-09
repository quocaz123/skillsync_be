package com.skillsync.skillsync.dto.request.forum;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerifyForumPostRequest {
    /** APPROVED hoặc REJECTED */
    @NotBlank(message = "action is required")
    String action;

    /** Bắt buộc khi action = REJECTED */
    String rejectionReason;
}