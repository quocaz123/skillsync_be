package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.request.skill.VerifyTeachingSkillRequest;
import com.skillsync.skillsync.dto.response.skill.AdminTeachingSkillResponse;
import com.skillsync.skillsync.enums.VerificationStatus;
import com.skillsync.skillsync.enums.LogLevel;
import com.skillsync.skillsync.service.AdminTeachingSkillService;
import com.skillsync.skillsync.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import com.skillsync.skillsync.dto.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/teaching-skills")
@RequiredArgsConstructor
public class AdminTeachingSkillController {

    private final AdminTeachingSkillService service;
    private final SystemLogService systemLogService;

    /**
     * GET /api/admin/teaching-skills?status=PENDING
     * Lấy danh sách teaching skills (lọc theo status nếu có).
     */
    @GetMapping
    public ApiResponse<List<AdminTeachingSkillResponse>> getAll(
            @RequestParam(required = false) VerificationStatus status
    ) {
        return ApiResponse.success(service.getAll(status));
    }

    /**
     * PATCH /api/admin/teaching-skills/{id}/verify
     * Body: { "action": "APPROVED" | "REJECTED", "rejectionReason": "..." }
     */
    @PatchMapping("/{id}/verify")
    public ApiResponse<AdminTeachingSkillResponse> verify(
            @PathVariable UUID id,
            @RequestBody VerifyTeachingSkillRequest request
    ) {
        AdminTeachingSkillResponse response = service.verify(id, request);
        systemLogService.logSystemEvent(
                "Duyet ky nang mentor: " + request.getAction()
                        + " | user=" + response.getUserEmail()
                        + " | skill=" + response.getSkillName(),
                LogLevel.INFO
        );
        return ApiResponse.success(response);
    }
}
