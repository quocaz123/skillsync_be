package com.skillsync.skillsync.dto.response.forum;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ForumCategoryResponse {
    UUID id;
    String name;
    String description;
    String icon;
    Integer displayOrder;
    LocalDateTime createdAt;
}
