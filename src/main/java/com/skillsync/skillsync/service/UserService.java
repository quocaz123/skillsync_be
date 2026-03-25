package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.request.UpdateAvatarRequest;
import com.skillsync.skillsync.dto.response.UserResponse;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.mapper.UserResponseMapper;
import com.skillsync.skillsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;
    private final UserResponseMapper userResponseMapper;

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public UserResponse getMe() {
        return userResponseMapper.toResponse(getCurrentUser());
    }

    public UserResponse updateAvatar(UpdateAvatarRequest request) {
        if (request.getAvatarUrl() == null || request.getAvatarUrl().isBlank())
            throw new IllegalArgumentException("avatarUrl không được để trống");
        if (request.getAvatarKey() == null || request.getAvatarKey().isBlank())
            throw new IllegalArgumentException("avatarKey không được để trống");

        User user = getCurrentUser();

        // Xóa avatar cũ trên S3 nếu có
        if (user.getAvatarKey() != null && !user.getAvatarKey().isBlank()) {
            fileUploadService.deleteFileByKey(user.getAvatarKey());
        }

        user.setAvatarUrl(request.getAvatarUrl());
        user.setAvatarKey(request.getAvatarKey());

        return userResponseMapper.toResponse(userRepository.save(user));
    }
}
