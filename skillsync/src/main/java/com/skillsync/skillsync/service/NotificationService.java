package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.request.notification.NotificationCreateRequest;
import com.skillsync.skillsync.dto.response.notification.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    NotificationResponse createAndSend(NotificationCreateRequest request);

    Page<NotificationResponse> getMyNotifications(Pageable pageable);

    long countMyUnread();

    void markAsRead(UUID notificationId);

    void markAllAsRead();
}
