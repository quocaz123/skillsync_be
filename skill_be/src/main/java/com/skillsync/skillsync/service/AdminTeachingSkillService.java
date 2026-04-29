package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.request.skill.VerifyTeachingSkillRequest;
import com.skillsync.skillsync.dto.response.skill.AdminTeachingSkillResponse;
import com.skillsync.skillsync.entity.TeachingSkillEvidence;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.entity.UserTeachingSkill;
import com.skillsync.skillsync.enums.VerificationStatus;
import com.skillsync.skillsync.exception.AppException;
import com.skillsync.skillsync.exception.ErrorCode;
import com.skillsync.skillsync.repository.TeachingSkillEvidenceRepository;
import com.skillsync.skillsync.repository.UserTeachingSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminTeachingSkillService {

    private final UserTeachingSkillRepository teachingSkillRepository;
    private final TeachingSkillEvidenceRepository evidenceRepository;
    private final UserService userService;
    private final NotificationEventPublisher notificationEventPublisher;

    /** Lấy tất cả teaching skills, lọc theo status nếu có */
    public List<AdminTeachingSkillResponse> getAll(VerificationStatus status) {
        List<UserTeachingSkill> list = (status != null)
                ? teachingSkillRepository.findByVerificationStatusOrderByCreatedAtAsc(status)
                : teachingSkillRepository.findAllByOrderByCreatedAtDesc();

        return list.stream().map(this::toResponse).toList();
    }

    /** Admin duyệt (APPROVED) hoặc từ chối (REJECTED) */
    @Transactional
    public AdminTeachingSkillResponse verify(UUID teachingSkillId, VerifyTeachingSkillRequest request) {
        UserTeachingSkill ts = teachingSkillRepository.findById(teachingSkillId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Teaching skill không tồn tại"));

        if (!"APPROVED".equalsIgnoreCase(request.getAction()) && !"REJECTED".equalsIgnoreCase(request.getAction()))
            throw new AppException(ErrorCode.INVALID_REQUEST, "action phải là APPROVED hoặc REJECTED");

        if ("REJECTED".equalsIgnoreCase(request.getAction()) &&
                (request.getRejectionReason() == null || request.getRejectionReason().isBlank()))
            throw new AppException(ErrorCode.INVALID_REQUEST, "rejectionReason bắt buộc khi từ chối");

        User admin = userService.getCurrentUser();

        ts.setVerificationStatus("APPROVED".equalsIgnoreCase(request.getAction())
                ? VerificationStatus.APPROVED
                : VerificationStatus.REJECTED);
        ts.setVerifiedBy(admin);
        ts.setVerifiedAt(LocalDateTime.now());
        ts.setRejectionReason("REJECTED".equalsIgnoreCase(request.getAction())
                ? request.getRejectionReason()
                : null);

        if ("APPROVED".equalsIgnoreCase(request.getAction())) {
            notificationEventPublisher.publishSkillEvent("SKILL_VERIFIED",
                    ts.getUser().getEmail(),
                    ts.getUser().getFullName(),
                    ts.getSkill().getName(),
                    null);
        } else {
            notificationEventPublisher.publishSkillEvent("SKILL_REJECTED",
                    ts.getUser().getEmail(),
                    ts.getUser().getFullName(),
                    ts.getSkill().getName(),
                    request.getRejectionReason());
        }

        return toResponse(teachingSkillRepository.save(ts));
    }

    private AdminTeachingSkillResponse toResponse(UserTeachingSkill ts) {
        List<TeachingSkillEvidence> evidences = evidenceRepository.findByTeachingSkillId(ts.getId());

        return AdminTeachingSkillResponse.builder()
                .id(ts.getId())
                .userId(ts.getUser().getId())
                .userEmail(ts.getUser().getEmail())
                .userFullName(ts.getUser().getFullName())
                .userAvatarUrl(ts.getUser().getAvatarUrl())
                .skillId(ts.getSkill().getId())
                .skillName(ts.getSkill().getName())
                .skillIcon(ts.getSkill().getIcon())
                .skillCategory(ts.getSkill().getCategory())
                .level(ts.getLevel())
                .experienceDesc(ts.getExperienceDesc())
                .outcomeDesc(ts.getOutcomeDesc())
                .creditsPerHour(ts.getCreditsPerHour())
                .verificationStatus(ts.getVerificationStatus())
                .rejectionReason(ts.getRejectionReason())
                .verifiedAt(ts.getVerifiedAt())
                .verifiedByEmail(ts.getVerifiedBy() != null ? ts.getVerifiedBy().getEmail() : null)
                .evidences(evidences.stream().map(ev ->
                        AdminTeachingSkillResponse.EvidenceSummary.builder()
                                .id(ev.getId())
                                .evidenceType(ev.getEvidenceType() != null ? ev.getEvidenceType().name() : null)
                                .title(ev.getTitle())
                                .fileUrl(ev.getFileUrl())
                                .externalUrl(ev.getExternalUrl())
                                .isVerified(ev.getIsVerified())
                                .build()
                ).toList())
                .createdAt(ts.getCreatedAt())
                .build();
    }
}
