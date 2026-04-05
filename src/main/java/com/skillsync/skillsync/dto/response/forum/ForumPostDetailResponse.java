package com.skillsync.skillsync.dto.response.forum;

import com.skillsync.skillsync.enums.PostType;
import com.skillsync.skillsync.enums.ForumPostStatus;
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
public class ForumPostDetailResponse {
    UUID id;
    UUID authorId;
    String authorName;
    String authorRole;
    String authorAvatar;
    UUID categoryId;
    String categoryName;
    String title;
    String content;
    PostType postType;
    List<String> tags;
    Long upvotes;
    Long downvotes;
    Long commentCount;
    Long saveCount;
    Boolean solved;
    Boolean liked;
    Boolean saved;
    List<CommentResponse> comments;
    ForumPostStatus status;
    String rejectionReason;
    LocalDateTime reviewedAt;
    String reviewedByEmail;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
