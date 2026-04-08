package com.skillsync.skillsync.mapper;

import com.skillsync.skillsync.dto.response.notification.NotificationResponse;
import com.skillsync.skillsync.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        if (notification == null) return null;
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .isRead(notification.getIsRead())
                .redirectUrl(notification.getRedirectUrl())
                .entityId(notification.getEntityId())
                .imageUrl(notification.getImageUrl())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
