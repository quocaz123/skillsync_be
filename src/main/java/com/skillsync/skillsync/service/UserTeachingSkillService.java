package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.request.skill.CreateTeachingSkillRequest;
import com.skillsync.skillsync.dto.response.skill.TeachingSkillResponse;
import com.skillsync.skillsync.entity.Skill;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.entity.UserTeachingSkill;
import com.skillsync.skillsync.repository.SkillRepository;
import com.skillsync.skillsync.repository.TeachingSkillEvidenceRepository;
import com.skillsync.skillsync.repository.UserTeachingSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserTeachingSkillService {

    private final UserTeachingSkillRepository teachingSkillRepository;
    private final SkillRepository skillRepository;
    private final TeachingSkillEvidenceRepository evidenceRepository;
    private final FileUploadService fileUploadService;
    private final UserService userService;

    public List<TeachingSkillResponse> getMyTeachingSkills() {
        User user = userService.getCurrentUser();
        return teachingSkillRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TeachingSkillResponse create(CreateTeachingSkillRequest request) {
        if (request.getSkillId() == null) throw new IllegalArgumentException("skillId không được để trống");
        if (request.getLevel() == null) throw new IllegalArgumentException("level không được để trống");
        if (request.getExperienceDesc() == null || request.getExperienceDesc().isBlank())
            throw new IllegalArgumentException("experienceDesc không được để trống");
        if (request.getOutcomeDesc() == null || request.getOutcomeDesc().isBlank())
            throw new IllegalArgumentException("outcomeDesc không được để trống");

        User user = userService.getCurrentUser();
        Skill skill = skillRepository.findById(request.getSkillId())
                .orElseThrow(() -> new RuntimeException("Skill không tồn tại"));

        if (teachingSkillRepository.existsByUserIdAndSkillIdAndLevel(user.getId(), skill.getId(), request.getLevel()))
            throw new IllegalStateException("Bạn đã đăng ký dạy " + skill.getName() + " ở level này rồi");

        UserTeachingSkill saved = teachingSkillRepository.save(UserTeachingSkill.builder()
                .user(user)
                .skill(skill)
                .level(request.getLevel())
                .experienceDesc(request.getExperienceDesc())
                .outcomeDesc(request.getOutcomeDesc())
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
            throw new RuntimeException("Bạn không có quyền xóa teaching skill này");

        // Xóa file evidence trên S3
        evidenceRepository.findByTeachingSkillId(id).forEach(ev -> {
            if (ev.getFileKey() != null && !ev.getFileKey().isBlank()) {
                fileUploadService.deleteFileByKey(ev.getFileKey());
            }
        });

        evidenceRepository.deleteByTeachingSkillId(id);
        teachingSkillRepository.delete(ts);
    }

    private TeachingSkillResponse toResponse(UserTeachingSkill ts) {
        return TeachingSkillResponse.builder()
                .id(ts.getId())
                .skillId(ts.getSkill().getId())
                .skillName(ts.getSkill().getName())
                .skillIcon(ts.getSkill().getIcon())
                .skillCategory(ts.getSkill().getCategory())
                .level(ts.getLevel())
                .experienceDesc(ts.getExperienceDesc())
                .outcomeDesc(ts.getOutcomeDesc())
                .creditsPerHour(ts.getCreditsPerHour())
                .verificationStatus(ts.getVerificationStatus())
                .createdAt(ts.getCreatedAt())
                .build();
    }
}
