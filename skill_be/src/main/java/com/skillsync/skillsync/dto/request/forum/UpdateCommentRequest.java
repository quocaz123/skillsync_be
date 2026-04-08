package com.skillsync.skillsync.dto.request.forum;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateCommentRequest {
    @Size(min = 1, max = 1000, message = "Comment must be between 1 and 1000 characters")
    String content;
}
