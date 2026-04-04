package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.request.user.UpdatePasswordRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.skillsync.skillsync.dto.request.upload.UpdateAvatarRequest;
import com.skillsync.skillsync.dto.request.user.UpdateBioRequest;
import com.skillsync.skillsync.dto.response.user.UserResponse;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.entity.UserLearningInterest;
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
    private final UserLearningInterestRepository learningInterestRepository;
    private final UserTeachingSkillRepository userTeachingSkillRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final PasswordEncoder passwordEncoder;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private UserResponse buildFullResponse(User user) {
        var id = user.getId();

        long teachingSessions  = sessionRepository.countByTeacherId(id);
        long learningSessions  = sessionRepository.countByLearnerId(id);
        Double avgRating       = reviewRepository.findAverageRatingByRevieweeId(id);
        long totalReviews      = reviewRepository.countByRevieweeId(id);
        long teachingSkillsCount = userTeachingSkillRepository.findByUserIdOrderByCreatedAtDesc(id).size();

        List<UserLearningInterest> interests = learningInterestRepository.findByUserId(id);
        List<UserResponse.LearningInterestSummary> interestSummaries = interests.stream()
                .map(i -> UserResponse.LearningInterestSummary.builder()
                        .skillName(i.getSkill() != null ? i.getSkill().getName() : null)
                        .skillIcon(i.getSkill() != null ? i.getSkill().getIcon() : null)
                        .desiredLevel(i.getDesiredLevel() != null ? i.getDesiredLevel().name() : null)
                        .learningGoal(i.getLearningGoal())
                        .build())
                .collect(Collectors.toList());

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
                .trustScore(user.getTrustScore())
                // stats
                .totalTeachingSessions(teachingSessions)
                .totalLearningSessions(learningSessions)
                .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : null)
                .totalReviews(totalReviews)
                .totalTeachingSkills(teachingSkillsCount)
                // interests
                .learningInterests(interestSummaries)
                .build();
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    public UserResponse getMe() {
        return buildFullResponse(getCurrentUser());
    }

    public List<CreditTransactionResponse> getMyTransactions() {
        User user = getCurrentUser();
        return creditTransactionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(tx -> CreditTransactionResponse.builder()
                        .id(tx.getId().toString())
                        .amount(tx.getAmount())
                        .transactionType(tx.getTransactionType())
                        .description(tx.getDescription())
                        .createdAt(tx.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public UserResponse updateAvatar(UpdateAvatarRequest request) {
        if (request.getFileKey() == null || request.getFileKey().isBlank())
            throw new IllegalArgumentException("fileKey không được để trống");

        User user = getCurrentUser();

        // Xóa avatar cũ trên S3 nếu có
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
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự.");
        }
        User user = getCurrentUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setHasPassword(true);
        return buildFullResponse(userRepository.save(user));
    }
}
