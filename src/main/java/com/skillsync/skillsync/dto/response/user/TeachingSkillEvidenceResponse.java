package com.skillsync.skillsync.dto.response.user;

import com.skillsync.skillsync.enums.EvidenceType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeachingSkillEvidenceResponse {
    UUID id;
    UUID teachingSkillId;
    EvidenceType evidenceType;
    String title;
    String fileName;
    String fileUrl;
    String fileKey;
    String mimeType;
    Long fileSize;
    String externalUrl;
    Boolean isVerified;
    LocalDateTime createdAt;
}
