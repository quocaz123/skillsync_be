package com.skillsync.skillsync.dto.request.forum;

import com.skillsync.skillsync.enums.PostType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateForumPostRequest {
    String title;
    String content;
    PostType postType;
    List<String> tags;
    Boolean solved;
}
