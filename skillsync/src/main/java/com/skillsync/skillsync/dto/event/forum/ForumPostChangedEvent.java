package com.skillsync.skillsync.dto.event.forum;

import com.skillsync.skillsync.enums.ForumPostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostChangedEvent {
    String action;
    UUID postId;
    String title;
    ForumPostStatus status;
    String rejectionReason;
    LocalDateTime reviewedAt;
    String reviewedByEmail;
    UUID authorId;
    UUID categoryId;
}