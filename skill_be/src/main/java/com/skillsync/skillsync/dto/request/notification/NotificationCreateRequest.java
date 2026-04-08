package com.skillsync.skillsync.dto.request.notification;

import com.skillsync.skillsync.enums.NotificationType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationCreateRequest {
    UUID userId;
    NotificationType type;
    String title;
    String content;
    String redirectUrl;
    UUID entityId;
    String imageUrl;
}
