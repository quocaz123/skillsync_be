package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.request.user.UpdatePasswordRequest;
import com.skillsync.skillsync.exception.AppException;
import com.skillsync.skillsync.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.skillsync.skillsync.dto.request.upload.UpdateAvatarRequest;
import com.skillsync.skillsync.dto.request.user.UpdateBioRequest;
import com.skillsync.skillsync.dto.response.user.UserResponse;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.repository.*;
import com.skillsync.skillsync.dto.response.user.CreditTransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;
    private final SessionRepository sessionRepository;
    private final ReviewRepository reviewRepository;
    private final UserTeachingSkillRepository userTeachingSkillRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final PasswordEncoder passwordEncoder;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new com.skillsync.skillsync.exception.AppException(com.skillsync.skillsync.exception.ErrorCode.UNAUTHORIZED));
    }

    private UserResponse buildFullResponse(User user) {
        var id = user.getId();

        long teachingSessions  = sessionRepository.countByTeacherId(id);
        long learningSessions  = sessionRepository.countByLearnerId(id);
        Double avgRating       = reviewRepository.findAverageRatingByRevieweeId(id);
        long totalReviews      = reviewRepository.countByRevieweeId(id);
        Long totalTeachingSkills = (long) userTeachingSkillRepository.findByUserIdOrderByCreatedAtDesc(id).size();

        Integer pendingLearner = sessionRepository.getLearnerPendingCredits(id);
        Integer pendingTeacher = sessionRepository.getTeacherPendingCredits(id);

        return UserResponse.builder()
                // identity
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .status(user.getStatus())
                .role(user.getRole())
                .hasPassword(user.getHasPassword() != null ? user.getHasPassword() : true)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                // gamification
                .creditsBalance(user.getCreditsBalance())
                .pendingLearnerCredits(pendingLearner)
                .pendingTeacherCredits(pendingTeacher)
                // stats
                .totalTeachingSessions(teachingSessions)
                .totalLearningSessions(learningSessions)
                .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : null)
                .totalReviews(totalReviews)
                .totalTeachingSkills(totalTeachingSkills)
                .build();
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    public UserResponse getMe() {
        return buildFullResponse(getCurrentUser());
    }

    /** Public profile của bất kỳ user nào — dùng cho trang mentor profile */
    public UserResponse getPublicProfile(java.util.UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.skillsync.skillsync.exception.AppException(
                        com.skillsync.skillsync.exception.ErrorCode.NOT_FOUND));
        return buildFullResponse(user);
    }

    public List<CreditTransactionResponse> getMyTransactions() {
        User user = getCurrentUser();
        return creditTransactionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(tx -> CreditTransactionResponse.builder()
                        .id(tx.getId())
                        .amount(tx.getAmount())
                        .transactionType(tx.getTransactionType())
                        .description(tx.getDescription())
                        .createdAt(tx.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public UserResponse updateAvatar(UpdateAvatarRequest request) {
        if (request.getFileKey() == null || request.getFileKey().isBlank())
            throw new AppException(ErrorCode.INVALID_REQUEST, "fileKey không được để trống");

        User user = getCurrentUser();

        // Xóa avatar cũ trên R2 nếu có
        if (user.getAvatarKey() != null && !user.getAvatarKey().isBlank()) {
            fileUploadService.deleteFileByKey(user.getAvatarKey());
        }

        user.setAvatarKey(request.getFileKey());
        user.setAvatarUrl(fileUploadService.buildPublicUrl(request.getFileKey()));

        return buildFullResponse(userRepository.save(user));
    }

    public UserResponse updateBio(UpdateBioRequest request) {
        User user = getCurrentUser();
        user.setBio(request.getBio());
        return buildFullResponse(userRepository.save(user));
    }

    public UserResponse setPassword(UpdatePasswordRequest request) {
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mật khẩu phải có ít nhất 8 ký tự.");
        }
        User user = getCurrentUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setHasPassword(true);
        return buildFullResponse(userRepository.save(user));
    }

    // ─── Admin API ───────────────────────────────────────────────────────────

    public List<com.skillsync.skillsync.dto.response.admin.AdminUserResponse> getAllUsersForAdmin() {
        return userRepository.findAll().stream()
                .map(u -> com.skillsync.skillsync.dto.response.admin.AdminUserResponse.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .fullName(u.getFullName())
                        .avatarUrl(u.getAvatarUrl())
                        .status(u.getStatus())
                        .role(u.getRole())
                        .creditsBalance(u.getCreditsBalance())
                        .createdAt(u.getCreatedAt())
                        .build()
                ).sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public com.skillsync.skillsync.dto.response.admin.AdminUserResponse toggleUserBanStatus(java.util.UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        if (user.getRole() == com.skillsync.skillsync.enums.Role.ADMIN) {
            throw new AppException(ErrorCode.FORBIDDEN, "Cannot ban an administrator.");
        }

        if (user.getStatus() == com.skillsync.skillsync.enums.UserStatus.BANNED) {
            user.setStatus(com.skillsync.skillsync.enums.UserStatus.ACTIVE);
        } else {
            user.setStatus(com.skillsync.skillsync.enums.UserStatus.BANNED);
        }

        userRepository.save(user);

        return com.skillsync.skillsync.dto.response.admin.AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .role(user.getRole())
                .creditsBalance(user.getCreditsBalance())
                .createdAt(user.getCreatedAt())
                .build();
    }
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::buildFullResponse)
                .collect(Collectors.toList());
    }
}
