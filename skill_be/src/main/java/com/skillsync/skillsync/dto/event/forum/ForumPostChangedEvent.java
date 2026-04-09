package com.skillsync.skillsync.dto.event.forum;

import com.skillsync.skillsync.enums.ForumPostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostChangedEvent {
    String action;
    UUID postId;
    String title;
    String content;
    ForumPostStatus status;
    String rejectionReason;
    LocalDateTime reviewedAt;
    String reviewedByEmail;
    UUID authorId;
    String authorName;
    String authorEmail;
    UUID categoryId;
    String categoryName;
    List<String> tags;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}