package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.request.upload.UpdateAvatarRequest;
import com.skillsync.skillsync.dto.response.user.UserResponse;
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
        if (request.getFileKey() == null || request.getFileKey().isBlank())
            throw new IllegalArgumentException("fileKey không được để trống");

        User user = getCurrentUser();

        // Xóa avatar cũ trên S3 nếu có
        if (user.getAvatarKey() != null && !user.getAvatarKey().isBlank()) {
            fileUploadService.deleteFileByKey(user.getAvatarKey());
        }

        String fileKey = request.getFileKey();
        user.setAvatarKey(fileKey);
        user.setAvatarUrl(fileUploadService.buildPublicUrl(fileKey)); // BE tự build URL

        return userResponseMapper.toResponse(userRepository.save(user));
    }
}
