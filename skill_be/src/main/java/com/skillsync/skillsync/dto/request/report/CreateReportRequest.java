package com.skillsync.skillsync.dto.request.report;

import com.skillsync.skillsync.enums.ReportReason;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateReportRequest {
    ReportReason reason;
    String description;
    String evidenceUrl;
}
