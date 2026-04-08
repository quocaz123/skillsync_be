package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.common.ApiResponse;
import com.skillsync.skillsync.dto.response.admin.AdminUserResponse;
import com.skillsync.skillsync.service.UserService;
import com.skillsync.skillsync.service.SystemLogService;
import com.skillsync.skillsync.enums.LogLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final SystemLogService systemLogService;

    @GetMapping
    public ApiResponse<List<AdminUserResponse>> getAllUsers() {
        return ApiResponse.success(userService.getAllUsersForAdmin());
    }

    @PatchMapping("/{userId}/toggle-ban")
    public ApiResponse<AdminUserResponse> toggleBanStatus(@PathVariable UUID userId) {
        AdminUserResponse res = userService.toggleUserBanStatus(userId);
        systemLogService.logSystemEvent("Thay đổi trạng thái khoá của người dùng: " + res.getEmail(), LogLevel.WARNING);
        return ApiResponse.success(res);
    }
}
