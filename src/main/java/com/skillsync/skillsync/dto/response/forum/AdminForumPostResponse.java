package com.skillsync.skillsync.dto.response.forum;

import com.skillsync.skillsync.enums.ForumPostStatus;
import com.skillsync.skillsync.enums.PostType;
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
public class AdminForumPostResponse {
    UUID id;
    UUID authorId;
    String authorName;
    String authorEmail;
    String authorRole;
    String authorAvatar;
    UUID categoryId;
    String categoryName;
    String title;
    String content;
    PostType postType;
    List<String> tags;
    ForumPostStatus status;
    String rejectionReason;
    LocalDateTime reviewedAt;
    String reviewedByEmail;
    Boolean solved;
    Long upvotes;
    Long downvotes;
    Long commentCount;
    Long saveCount;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}