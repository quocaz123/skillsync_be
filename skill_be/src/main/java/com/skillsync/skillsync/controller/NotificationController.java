package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.common.ApiResponse;
import com.skillsync.skillsync.dto.response.notification.NotificationResponse;
import com.skillsync.skillsync.dto.response.notification.UnreadCountResponse;
import com.skillsync.skillsync.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<Page<NotificationResponse>> getMyNotifications(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(notificationService.getMyNotifications(pageable));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount() {
        return ApiResponse.success(new UnreadCountResponse(notificationService.countMyUnread()));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ApiResponse.success(null);
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ApiResponse.success(null);
    }
}
