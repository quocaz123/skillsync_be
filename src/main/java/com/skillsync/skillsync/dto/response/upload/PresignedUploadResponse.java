package com.skillsync.skillsync.dto.response.upload;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PresignedUploadResponse {
    String uploadUrl;
    String fileUrl;
    String fileKey;
    Long expiresInSeconds;
}
