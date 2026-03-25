package com.skillsync.skillsync.dto.request;

import com.skillsync.skillsync.enums.EvidenceType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeachingSkillEvidenceRequest {
    EvidenceType evidenceType;
    String title;
    String fileName;
    String fileUrl;
    String fileKey;
    String mimeType;
    Long fileSize;
    String externalUrl;
}
