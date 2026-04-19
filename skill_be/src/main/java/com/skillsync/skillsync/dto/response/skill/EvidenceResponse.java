package com.skillsync.skillsync.dto.response.skill;

import com.skillsync.skillsync.enums.EvidenceType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EvidenceResponse {
    UUID id;
    EvidenceType evidenceType;
    String title;
    String description;
    String fileUrl;
    String externalUrl;
    Boolean isVerified;
}
