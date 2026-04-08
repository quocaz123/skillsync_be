package com.skillsync.skillsync.dto.request.forum;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateCategoryRequest {
    String name;
    String description;
    String icon;
    Integer displayOrder;
}
