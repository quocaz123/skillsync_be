package com.skillsync.skillsync.dto.response.notification;

import com.skillsync.skillsync.enums.NotificationType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationResponse {
    UUID id;
    NotificationType type;
    String title;
    String content;
    Boolean isRead;
    String redirectUrl;
    UUID entityId;
    String imageUrl;
    LocalDateTime createdAt;
}
