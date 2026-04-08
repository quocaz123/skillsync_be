package com.skillsync.skillsync.dto.response.forum;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentResponse {
    UUID id;
    UUID postId;
    UUID parentCommentId;
    UUID authorId;
    String authorName;
    String authorRole;
    String authorAvatar;
    String content;
    Long likeCount;
    Boolean liked;
    List<CommentResponse> replies;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
