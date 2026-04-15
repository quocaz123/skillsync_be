package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.request.notification.NotificationCreateRequest;
import com.skillsync.skillsync.dto.response.notification.NotificationResponse;
import com.skillsync.skillsync.entity.Notification;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.mapper.NotificationMapper;
import com.skillsync.skillsync.repository.NotificationRepository;
import com.skillsync.skillsync.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationService {

    NotificationRepository notificationRepository;
    UserRepository userRepository;
    NotificationMapper notificationMapper;
    SimpMessagingTemplate messagingTemplate;
    UserService userService;

    @Transactional
    public NotificationResponse createAndSend(NotificationCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getUserId()));

        Notification notification = Notification.builder()
                .user(user)
                .type(request.getType())
                .title(request.getTitle())
                .content(request.getContent())
                .isRead(false)
                .redirectUrl(request.getRedirectUrl())
                .entityId(request.getEntityId())
                .imageUrl(request.getImageUrl())
                .build();

        notification = notificationRepository.save(notification);
        NotificationResponse response = notificationMapper.toResponse(notification);

        try {
            messagingTemplate.convertAndSendToUser(
                    user.getEmail(),
                    "/queue/notifications",
                    response
            );

            messagingTemplate.convertAndSendToUser(
                    user.getEmail(),
                    "/queue/notifications/unread-count",
                    countUnreadByUserId(user.getId())
            );
        } catch (Exception e) {
            System.err.println("Failed to send WebSocket notification to user " + user.getEmail() + ": " + e.getMessage());
        }

        return response;
    }

    public Page<NotificationResponse> getMyNotifications(Pageable pageable) {
        User currentUser = userService.getCurrentUser();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId(), pageable)
                .map(notificationMapper::toResponse);
    }

    public long countMyUnread() {
        User currentUser = userService.getCurrentUser();
        return notificationRepository.countByUserIdAndIsReadFalse(currentUser.getId());
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        User currentUser = userService.getCurrentUser();

        Notification notification = notificationRepository.findByIdAndUserId(notificationId, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notificationRepository.save(notification);

            try {
                messagingTemplate.convertAndSendToUser(
                        currentUser.getEmail(),
                        "/queue/notifications/unread-count",
                        countUnreadByUserId(currentUser.getId())
                );
            } catch (Exception e) {
                // Ignore disconnect issues
            }
        }
    }

    @Transactional
    public void markAllAsRead() {
        User currentUser = userService.getCurrentUser();

        notificationRepository.markAllReadByUserId(currentUser.getId());

        try {
            messagingTemplate.convertAndSendToUser(
                    currentUser.getEmail(),
                    "/queue/notifications/unread-count",
                    0L
            );
        } catch (Exception e) {
            // Ignore disconnect issues
        }
    }

    private long countUnreadByUserId(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
}
