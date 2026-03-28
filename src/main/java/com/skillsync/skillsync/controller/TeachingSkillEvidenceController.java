package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.request.skill.TeachingSkillEvidenceRequest;
import com.skillsync.skillsync.dto.response.user.TeachingSkillEvidenceResponse;
import com.skillsync.skillsync.service.TeachingSkillEvidenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import com.skillsync.skillsync.dto.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teaching-skill-evidences")
@RequiredArgsConstructor
public class TeachingSkillEvidenceController {

    private final TeachingSkillEvidenceService service;

    @PostMapping("/{teachingSkillId}")
    public ApiResponse<TeachingSkillEvidenceResponse> create(
            @PathVariable UUID teachingSkillId,
            @RequestBody TeachingSkillEvidenceRequest request
    ) {
        return ApiResponse.success(service.create(teachingSkillId, request));
    }

    @GetMapping("/teaching-skill/{teachingSkillId}")
    public ApiResponse<List<TeachingSkillEvidenceResponse>> getByTeachingSkill(@PathVariable UUID teachingSkillId) {
        return ApiResponse.success(service.getByTeachingSkill(teachingSkillId));
    }

    @DeleteMapping("/{evidenceId}")
    public ApiResponse<Void> delete(@PathVariable UUID evidenceId) {
        service.delete(evidenceId);
        return ApiResponse.success(null);
    }
}
