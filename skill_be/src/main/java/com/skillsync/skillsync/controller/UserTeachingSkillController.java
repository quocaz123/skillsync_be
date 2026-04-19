package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.common.PageResponse;
import com.skillsync.skillsync.dto.request.skill.CreateTeachingSkillRequest;
import com.skillsync.skillsync.dto.request.skill.UpdateSkillPriceRequest;
import com.skillsync.skillsync.dto.response.skill.TeachingSkillResponse;
import com.skillsync.skillsync.enums.SkillCategory;
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

    /**
     * Explore — tìm kiếm / lọc / phân trang phía server (tránh tải full list rồi filter client).
     * sort: {@code newest} | {@code credits_asc} | {@code credits_desc} | {@code experience}
     */
    @GetMapping("/explore")
    public ApiResponse<PageResponse<TeachingSkillResponse>> explore(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID skillId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size
    ) {
        SkillCategory cat = parseCategory(category);
        return ApiResponse.success(service.exploreTeachingSkills(q, skillId, cat, sort, page, size));
    }

    private static SkillCategory parseCategory(String category) {
        if (category == null || category.isBlank()) return null;
        try {
            return SkillCategory.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
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

    @PutMapping("/{id}/price")
    public ApiResponse<TeachingSkillResponse> updatePrice(@PathVariable UUID id, @RequestBody UpdateSkillPriceRequest request) {
        return ApiResponse.success(service.updatePrice(id, request.getNewPrice()));
    }

    /** Mentor bật/tắt hiển thị công khai (không xóa dữ liệu). */
    @PostMapping("/{id}/toggle-visibility")
    public ApiResponse<TeachingSkillResponse> toggleVisibility(@PathVariable UUID id) {
        return ApiResponse.success(service.toggleVisibility(id));
    }
}
