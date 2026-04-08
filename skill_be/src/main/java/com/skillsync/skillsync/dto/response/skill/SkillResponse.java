package com.skillsync.skillsync.dto.response.skill;

import com.skillsync.skillsync.enums.SkillCategory;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SkillResponse {
    UUID id;
    String name;
    SkillCategory category;
    String icon;
}
