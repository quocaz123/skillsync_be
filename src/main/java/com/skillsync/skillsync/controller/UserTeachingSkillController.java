package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.request.skill.CreateTeachingSkillRequest;
import com.skillsync.skillsync.dto.response.skill.TeachingSkillResponse;
import com.skillsync.skillsync.service.UserTeachingSkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import com.skillsync.skillsync.dto.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teaching-skills")
@RequiredArgsConstructor
public class UserTeachingSkillController {

    private final UserTeachingSkillService service;

    @GetMapping("/me")
    public ApiResponse<List<TeachingSkillResponse>> getMyTeachingSkills() {
        return ApiResponse.success(service.getMyTeachingSkills());
    }

    /** Public endpoint — Explore page lists all approved mentors */
    @GetMapping("/approved")
    public ApiResponse<List<TeachingSkillResponse>> getApproved() {
        return ApiResponse.success(service.getApprovedTeachingSkills());
    }

    @PostMapping
    public ApiResponse<TeachingSkillResponse> create(@RequestBody CreateTeachingSkillRequest request) {
        return ApiResponse.success(service.create(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.success(null);
    }
}
