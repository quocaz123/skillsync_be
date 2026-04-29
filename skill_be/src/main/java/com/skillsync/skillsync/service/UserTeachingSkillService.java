package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.common.PageResponse;
import com.skillsync.skillsync.dto.request.skill.CreateTeachingSkillRequest;
import com.skillsync.skillsync.dto.response.skill.TeachingSkillResponse;
import com.skillsync.skillsync.entity.Skill;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.entity.UserTeachingSkill;
import com.skillsync.skillsync.enums.SkillCategory;
import com.skillsync.skillsync.enums.VerificationStatus;
import com.skillsync.skillsync.repository.SkillRepository;
import com.skillsync.skillsync.repository.TeachingSkillEvidenceRepository;
import com.skillsync.skillsync.repository.UserTeachingSkillExploreSpec;
import com.skillsync.skillsync.repository.UserTeachingSkillRepository;
import com.skillsync.skillsync.repository.TeachingSlotRepository;
import com.skillsync.skillsync.repository.ReviewRepository;
import com.skillsync.skillsync.entity.TeachingSkillEvidence;
import com.skillsync.skillsync.entity.Review;
import com.skillsync.skillsync.dto.response.skill.EvidenceResponse;
import com.skillsync.skillsync.dto.response.review.ReviewResponse;
import com.skillsync.skillsync.exception.AppException;
import com.skillsync.skillsync.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserTeachingSkillService {

    private final UserTeachingSkillRepository teachingSkillRepository;
    private final SkillRepository skillRepository;
    private final TeachingSkillEvidenceRepository evidenceRepository;
    private final FileUploadService fileUploadService;
    private final TeachingSlotRepository teachingSlotRepository;
    private final ReviewRepository reviewRepository;
    private final UserService userService;

    public List<TeachingSkillResponse> getMyTeachingSkills() {
        User user = userService.getCurrentUser();
        return teachingSkillRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Public — APPROVED và chưa bị mentor tạm ẩn (Explore / AI), không lọc phân trang */
    @Transactional(readOnly = true)
    public List<TeachingSkillResponse> getApprovedTeachingSkills() {
        List<UserTeachingSkill> skills = teachingSkillRepository
                .findByVerificationStatusAndHiddenFalse(VerificationStatus.APPROVED);
        return enrichTeachingSkills(skills);
    }

    /**
     * Explore: lọc + phân trang phía server; enrich theo batch (stats / evidences / reviews) — tránh N+1.
     *
     * @param sort {@code newest} | {@code credits_asc} | {@code credits_desc} | {@code experience}
     */
    @Transactional(readOnly = true)
    public PageResponse<TeachingSkillResponse> exploreTeachingSkills(
            String q,
            UUID skillId,
            SkillCategory category,
            String sort,
            int page,
            int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);

        Specification<UserTeachingSkill> spec = UserTeachingSkillExploreSpec.approvedPublic(q, skillId, category);

        if ("experience".equalsIgnoreCase(sort)) {
            List<UserTeachingSkill> all = teachingSkillRepository.findAll(spec);
            if (all.isEmpty()) {
                return PageResponse.<TeachingSkillResponse>builder()
                        .currentPage(safePage)
                        .totalPages(0)
                        .pageSize(safeSize)
                        .totalElements(0)
                        .data(List.of())
                        .build();
            }

            List<UUID> allIds = all.stream().map(UserTeachingSkill::getId).toList();
            Map<UUID, TeachingSlotRepository.TeachingSkillStats> statsMap = teachingSlotRepository
                    .getStatsBySkillIds(allIds).stream()
                    .collect(Collectors.toMap(TeachingSlotRepository.TeachingSkillStats::getTeachingSkillId, s -> s, (a, b) -> a));

            List<UserTeachingSkill> sorted = all.stream()
                    .sorted(Comparator.comparingLong((UserTeachingSkill uts) -> {
                        TeachingSlotRepository.TeachingSkillStats st = statsMap.get(uts.getId());
                        return st != null && st.getTotalSessions() != null ? st.getTotalSessions() : 0L;
                    }).reversed().thenComparing(uts -> uts.getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();

            long total = sorted.size();
            int totalPages = (int) Math.ceil(total / (double) safeSize);
            int from = safePage * safeSize;
            if (from >= total) {
                return PageResponse.<TeachingSkillResponse>builder()
                        .currentPage(safePage)
                        .totalPages(totalPages)
                        .pageSize(safeSize)
                        .totalElements(total)
                        .data(List.of())
                        .build();
            }
            int to = Math.min(from + safeSize, (int) total);
            List<UserTeachingSkill> slice = sorted.subList(from, to);
            return PageResponse.<TeachingSkillResponse>builder()
                    .currentPage(safePage)
                    .totalPages(totalPages)
                    .pageSize(safeSize)
                    .totalElements(total)
                    .data(enrichTeachingSkills(slice))
                    .build();
        }

        Sort sortObj = resolveExploreSort(sort);
        Pageable pageable = PageRequest.of(safePage, safeSize, sortObj);
        Page<UserTeachingSkill> p = teachingSkillRepository.findAll(spec, pageable);
        return PageResponse.<TeachingSkillResponse>builder()
                .currentPage(p.getNumber())
                .totalPages(p.getTotalPages())
                .pageSize(p.getSize())
                .totalElements(p.getTotalElements())
                .data(enrichTeachingSkills(p.getContent()))
                .build();
    }

    private static Sort resolveExploreSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sort.toLowerCase()) {
            case "credits_asc" -> Sort.by(Sort.Direction.ASC, "creditsPerHour");
            case "credits_desc" -> Sort.by(Sort.Direction.DESC, "creditsPerHour");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    /**
     * Gom stats + evidences + reviews theo danh sách id (batch IN) — không gọi DB trong vòng lặp từng dòng.
     */
    private List<TeachingSkillResponse> enrichTeachingSkills(List<UserTeachingSkill> skills) {
        if (skills.isEmpty()) return List.of();

        List<UUID> skillIds = skills.stream().map(UserTeachingSkill::getId).toList();
        Map<UUID, TeachingSlotRepository.TeachingSkillStats> statsMap = teachingSlotRepository
                .getStatsBySkillIds(skillIds).stream()
                .collect(Collectors.toMap(TeachingSlotRepository.TeachingSkillStats::getTeachingSkillId, s -> s));

        Map<UUID, List<TeachingSkillEvidence>> evidencesMap = evidenceRepository.findByTeachingSkillIdIn(skillIds)
                .stream().collect(Collectors.groupingBy(e -> e.getTeachingSkill().getId()));

        Map<UUID, List<Review>> reviewsMap = reviewRepository.findLearnerReviewsByTeachingSkillIds(skillIds)
                .stream().collect(Collectors.groupingBy(r -> r.getSession().getTeachingSkill().getId()));

        return skills.stream()
                .map(ts -> {
                    TeachingSlotRepository.TeachingSkillStats stat = statsMap.get(ts.getId());
                    List<EvidenceResponse> evs = evidencesMap.getOrDefault(ts.getId(), List.of())
                            .stream().map(this::evidenceToResponse).toList();
                    List<ReviewResponse> revs = reviewsMap.getOrDefault(ts.getId(), List.of())
                            .stream().map(this::reviewToResponse).toList();
                    
                    double avg = revs.stream().mapToInt(ReviewResponse::getRating).average().orElse(0.0);
                    int total = revs.size();
                    
                    return toResponseFully(ts, stat != null ? stat.getOpenSlots() : 0L, stat != null ? stat.getTotalSessions() : 0L, evs, revs, total, avg);
                })
                .toList();
    }

    public TeachingSkillResponse create(CreateTeachingSkillRequest request) {
        if (request.getSkillId() == null) throw new AppException(ErrorCode.INVALID_REQUEST, "skillId không được để trống");
        if (request.getLevel() == null) throw new AppException(ErrorCode.INVALID_REQUEST, "level không được để trống");
        if (request.getExperienceDesc() == null || request.getExperienceDesc().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "experienceDesc không được để trống");
        }
        if (request.getOutcomeDesc() == null || request.getOutcomeDesc().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "outcomeDesc không được để trống");
        }
        User user = userService.getCurrentUser();
        Skill skill = skillRepository.findById(request.getSkillId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Skill không tồn tại"));

        if (teachingSkillRepository.existsByUserIdAndSkillIdAndLevel(user.getId(), skill.getId(), request.getLevel())) {
            throw new AppException(ErrorCode.TEACHING_SKILL_DUPLICATE,
                    "Bạn đã đăng ký dạy " + skill.getName() + " ở level này rồi");
        }

        UserTeachingSkill saved = teachingSkillRepository.save(UserTeachingSkill.builder()
                .user(user)
                .skill(skill)
                .level(request.getLevel())
                .experienceDesc(request.getExperienceDesc())
                .outcomeDesc(request.getOutcomeDesc())
                .teachingStyle(request.getTeachingStyle())
                .creditsPerHour(request.getCreditsPerHour() != null ? request.getCreditsPerHour() : 12)
                .build());

        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        UserTeachingSkill ts = teachingSkillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Teaching skill không tồn tại"));

        User user = userService.getCurrentUser();
        if (!ts.getUser().getId().equals(user.getId()))
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền xóa teaching skill này");

        // Xóa file evidence trên R2
        evidenceRepository.findByTeachingSkillId(id).forEach(ev -> {
            if (ev.getFileKey() != null && !ev.getFileKey().isBlank()) {
                fileUploadService.deleteFileByKey(ev.getFileKey());
            }
        });

        evidenceRepository.deleteByTeachingSkillId(id);
        teachingSkillRepository.delete(ts);
    }

    @Transactional
    public TeachingSkillResponse updatePrice(UUID id, int newPrice) {
        UserTeachingSkill ts = teachingSkillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Teaching skill không tồn tại"));

        User user = userService.getCurrentUser();
        if (!ts.getUser().getId().equals(user.getId()))
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền sửa giá teaching skill này");

        ts.setCreditsPerHour(newPrice);
        teachingSkillRepository.save(ts);
        
        return toResponse(ts);
    }

    @Transactional
    public TeachingSkillResponse toggleVisibility(UUID id) {
        UserTeachingSkill ts = teachingSkillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Teaching skill không tồn tại"));

        User user = userService.getCurrentUser();
        if (!ts.getUser().getId().equals(user.getId()))
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền thay đổi kỹ năng này");

        if (ts.getVerificationStatus() != com.skillsync.skillsync.enums.VerificationStatus.APPROVED) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Chỉ kỹ năng đã duyệt mới có thể tạm ẩn/hiện");
        }

        ts.setHidden(!ts.isHidden());
        teachingSkillRepository.save(ts);
        return toResponse(ts);
    }

    private EvidenceResponse evidenceToResponse(TeachingSkillEvidence ev) {
        return EvidenceResponse.builder()
                .id(ev.getId())
                .evidenceType(ev.getEvidenceType())
                .title(ev.getTitle())
                .description(ev.getDescription())
                .fileUrl(ev.getFileUrl())
                .externalUrl(ev.getExternalUrl())
                .isVerified(ev.getIsVerified())
                .build();
    }

    private ReviewResponse reviewToResponse(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .reviewerId(r.getReviewer().getId())
                .reviewerName(r.getReviewer().getFullName())
                .reviewerAvatar(r.getReviewer().getAvatarUrl())
                .sessionId(r.getSession().getId())
                .skillName(r.getSession().getTeachingSkill().getSkill().getName())
                .build();
    }

    private TeachingSkillResponse toResponse(UserTeachingSkill ts) {
        List<EvidenceResponse> evs = evidenceRepository.findByTeachingSkillId(ts.getId())
                .stream().map(this::evidenceToResponse).toList();
        return toResponseFully(ts, 0L, 0L, evs, java.util.List.of(), 0, 0.0);
    }

    private TeachingSkillResponse toResponseFully(UserTeachingSkill ts, Long openSlots, Long totalSessions, List<EvidenceResponse> evidences, List<ReviewResponse> reviews, Integer totalReviews, Double averageRating) {
        return TeachingSkillResponse.builder()
                .id(ts.getId())
                .skillId(ts.getSkill().getId())
                .skillName(ts.getSkill().getName())
                .skillIcon(ts.getSkill().getIcon())
                .skillCategory(ts.getSkill().getCategory())
                .level(ts.getLevel())
                .experienceDesc(ts.getExperienceDesc())
                .outcomeDesc(ts.getOutcomeDesc())
                .teachingStyle(ts.getTeachingStyle())
                .creditsPerHour(ts.getCreditsPerHour())
                .verificationStatus(ts.getVerificationStatus())
                .rejectionReason(ts.getRejectionReason())
                .hidden(ts.isHidden())
                .openSlotsCount(openSlots)
                .totalSessions(totalSessions)
                .totalReviews(totalReviews)
                .averageRating(averageRating)
                .teacherId(ts.getUser().getId())
                .teacherName(ts.getUser().getFullName())
                .teacherAvatar(ts.getUser().getAvatarUrl())
                .teacherBio(ts.getUser().getBio())
                .evidences(evidences)
                .reviews(reviews)
                .createdAt(ts.getCreatedAt())
                .build();
    }
}
