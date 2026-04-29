package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.common.ApiResponse;
import com.skillsync.skillsync.dto.request.upload.UpdateAvatarRequest;
import com.skillsync.skillsync.dto.request.user.UpdateBioRequest;
import com.skillsync.skillsync.dto.request.user.UpdatePasswordRequest;
import com.skillsync.skillsync.dto.response.user.UserResponse;
import com.skillsync.skillsync.dto.response.user.CreditTransactionResponse;
import com.skillsync.skillsync.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<List<UserResponse>> getAllUsers() {
        return ApiResponse.success(userService.getAllUsers());
    }

    /** Lấy profile đầy đủ của user hiện tại */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe() {
        return ApiResponse.success(userService.getMe());
    }

    /** Lấy lịch sử giao dịch credit */
    @GetMapping("/me/transactions")
    public ApiResponse<List<CreditTransactionResponse>> getMyTransactions() {
        return ApiResponse.success(userService.getMyTransactions());
    }

    /** Cập nhật avatar */
    @PatchMapping("/me/avatar")
    public ApiResponse<UserResponse> updateAvatar(@RequestBody UpdateAvatarRequest request) {
        return ApiResponse.success(userService.updateAvatar(request));
    }

    /** Cập nhật bio */
    @PatchMapping("/me/bio")
    public ApiResponse<UserResponse> updateBio(@RequestBody UpdateBioRequest request) {
        return ApiResponse.success(userService.updateBio(request));
    }

    /** Đặt mật khẩu lần đầu — dành cho người dùng đăng nhập Google chưa có password */
    @PatchMapping("/me/password")
    public ApiResponse<UserResponse> setPassword(@RequestBody UpdatePasswordRequest request) {
        return ApiResponse.success(userService.setPassword(request));
    }

    /** Lấy public profile của bất kỳ user nào (hiển thị trang Mentor) */
    @GetMapping("/{id}/profile")
    public ApiResponse<UserResponse> getPublicProfile(@PathVariable java.util.UUID id) {
        return ApiResponse.success(userService.getPublicProfile(id));
    }
}
