package com.skillsync.skillsync.dto.request.report;

import com.skillsync.skillsync.enums.ReportStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResolveReportRequest {
    ReportStatus resolution;
    String adminNotes;
}
