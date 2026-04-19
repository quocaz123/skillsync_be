package com.skillsync.skillsync.dto.request.report;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmitCounterEvidenceRequest {
    String description;
    String evidenceUrl;
}
