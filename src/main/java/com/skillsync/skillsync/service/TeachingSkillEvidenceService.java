package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.request.TeachingSkillEvidenceRequest;
import com.skillsync.skillsync.dto.response.TeachingSkillEvidenceResponse;
import com.skillsync.skillsync.entity.TeachingSkillEvidence;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.entity.UserTeachingSkill;
import com.skillsync.skillsync.enums.VerificationStatus;
import com.skillsync.skillsync.mapper.TeachingSkillEvidenceMapper;
import com.skillsync.skillsync.repository.TeachingSkillEvidenceRepository;
import com.skillsync.skillsync.repository.UserTeachingSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeachingSkillEvidenceService {

    private final TeachingSkillEvidenceRepository evidenceRepository;
    private final UserTeachingSkillRepository teachingSkillRepository;
    private final TeachingSkillEvidenceMapper evidenceMapper;
    private final UserService userService;
    private final FileUploadService fileUploadService;

    public TeachingSkillEvidenceResponse create(UUID teachingSkillId, TeachingSkillEvidenceRequest request) {
        UserTeachingSkill teachingSkill = teachingSkillRepository.findById(teachingSkillId)
                .orElseThrow(() -> new RuntimeException("Teaching skill not found"));

        User currentUser = userService.getCurrentUser();

        if (!teachingSkill.getUser().getId().equals(currentUser.getId()))
            throw new RuntimeException("Bạn không có quyền thêm minh chứng cho kỹ năng này");

        validateEvidenceRequest(request);

        TeachingSkillEvidence evidence = evidenceMapper.toEntity(request);
        evidence.setTeachingSkill(teachingSkill);
        evidence.setIsVerified(false);

        TeachingSkillEvidence saved = evidenceRepository.save(evidence);

        // Reset về PENDING nếu trước đó bị REJECTED
        if (teachingSkill.getVerificationStatus() == VerificationStatus.REJECTED) {
            teachingSkill.setVerificationStatus(VerificationStatus.PENDING);
            teachingSkillRepository.save(teachingSkill);
        }

        return evidenceMapper.toResponse(saved);
    }

    public List<TeachingSkillEvidenceResponse> getByTeachingSkill(UUID teachingSkillId) {
        return evidenceRepository.findByTeachingSkillId(teachingSkillId)
                .stream()
                .map(evidenceMapper::toResponse)
                .toList();
    }

    public void delete(UUID evidenceId) {
        TeachingSkillEvidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new RuntimeException("Evidence not found"));

        User currentUser = userService.getCurrentUser();

        if (!evidence.getTeachingSkill().getUser().getId().equals(currentUser.getId()))
            throw new RuntimeException("Bạn không có quyền xóa minh chứng này");

        if (evidence.getFileKey() != null && !evidence.getFileKey().isBlank()) {
            fileUploadService.deleteFileByKey(evidence.getFileKey());
        }

        evidenceRepository.delete(evidence);
    }

    private void validateEvidenceRequest(TeachingSkillEvidenceRequest request) {
        if (request == null) throw new IllegalArgumentException("Request không được null");
        if (request.getEvidenceType() == null)
            throw new IllegalArgumentException("evidenceType không được để trống");
        if (request.getTitle() == null || request.getTitle().isBlank())
            throw new IllegalArgumentException("title không được để trống");

        boolean hasFile = request.getFileUrl() != null && !request.getFileUrl().isBlank();
        boolean hasLink = request.getExternalUrl() != null && !request.getExternalUrl().isBlank();

        if (!hasFile && !hasLink)
            throw new IllegalArgumentException("Phải có file hoặc externalUrl");
    }
}
