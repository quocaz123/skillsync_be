package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.request.skill.VerifyTeachingSkillRequest;
import com.skillsync.skillsync.dto.response.skill.AdminTeachingSkillResponse;
import com.skillsync.skillsync.enums.VerificationStatus;
import com.skillsync.skillsync.service.AdminTeachingSkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/teaching-skills")
@RequiredArgsConstructor
public class AdminTeachingSkillController {

    private final AdminTeachingSkillService service;

    /**
     * GET /api/admin/teaching-skills?status=PENDING
     * Lấy danh sách teaching skills (lọc theo status nếu có).
     */
    @GetMapping
    public List<AdminTeachingSkillResponse> getAll(
            @RequestParam(required = false) VerificationStatus status
    ) {
        return service.getAll(status);
    }

    /**
     * PATCH /api/admin/teaching-skills/{id}/verify
     * Body: { "action": "APPROVED" | "REJECTED", "rejectionReason": "..." }
     */
    @PatchMapping("/{id}/verify")
    public AdminTeachingSkillResponse verify(
            @PathVariable UUID id,
            @RequestBody VerifyTeachingSkillRequest request
    ) {
        return service.verify(id, request);
    }
}
