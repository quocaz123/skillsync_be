package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.request.skill.TeachingSkillEvidenceRequest;
import com.skillsync.skillsync.dto.response.user.TeachingSkillEvidenceResponse;
import com.skillsync.skillsync.service.TeachingSkillEvidenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teaching-skill-evidences")
@RequiredArgsConstructor
public class TeachingSkillEvidenceController {

    private final TeachingSkillEvidenceService service;

    @PostMapping("/{teachingSkillId}")
    @ResponseStatus(HttpStatus.CREATED)
    public TeachingSkillEvidenceResponse create(
            @PathVariable UUID teachingSkillId,
            @RequestBody TeachingSkillEvidenceRequest request
    ) {
        return service.create(teachingSkillId, request);
    }

    @GetMapping("/teaching-skill/{teachingSkillId}")
    public List<TeachingSkillEvidenceResponse> getByTeachingSkill(@PathVariable UUID teachingSkillId) {
        return service.getByTeachingSkill(teachingSkillId);
    }

    @DeleteMapping("/{evidenceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID evidenceId) {
        service.delete(evidenceId);
    }
}
