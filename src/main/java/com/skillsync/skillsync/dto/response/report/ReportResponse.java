package com.skillsync.skillsync.dto.response.report;

import com.skillsync.skillsync.enums.ReportReason;
import com.skillsync.skillsync.enums.ReportStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportResponse {
    UUID id;
    UUID sessionId;
    UUID reporterId;
    String reporterName;
    UUID reportedUserId;
    String reportedUserName;
    ReportReason reason;
    String description;
    String evidenceUrl;
    ReportStatus status;
    String adminNotes;
    LocalDateTime resolvedAt;
    LocalDateTime createdAt;
}
