package com.skillsync.skillsync.dto.request.upload;

import com.skillsync.skillsync.enums.UploadType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PresignedUploadRequest {
    String fileName;
    String contentType;
    UploadType uploadType;
}
