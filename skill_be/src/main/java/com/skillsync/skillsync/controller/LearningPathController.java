package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.common.ApiResponse;
import com.skillsync.skillsync.dto.request.learningpath.LearningPathCreateRequest;
import com.skillsync.skillsync.dto.response.learningpath.LearningPathEnrollResponse;
import com.skillsync.skillsync.dto.response.learningpath.LearningPathResponse;
import com.skillsync.skillsync.service.LearningPathService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/learning-paths")
@RequiredArgsConstructor
public class LearningPathController {

    private final LearningPathService learningPathService;

    /** Public — Khám phá: chỉ trả APPROVED paths */
    @GetMapping("/approved")
    public ApiResponse<List<LearningPathResponse>> getApproved() {
        return ApiResponse.success(learningPathService.getApproved());
    }

    /** Mentor — lộ trình của mình */
    @GetMapping("/my")
    public ApiResponse<List<LearningPathResponse>> getMy() {
        return ApiResponse.success(learningPathService.getMyPaths());
    }

    /** Learner — các lộ trình đã đăng ký */
    @GetMapping("/enrolled")
    public ApiResponse<List<LearningPathResponse>> getEnrolled() {
        return ApiResponse.success(learningPathService.getEnrolledPaths());
    }

    /** Admin — toàn bộ lộ trình */
    @GetMapping
    public ApiResponse<List<LearningPathResponse>> getAll() {
        return ApiResponse.success(learningPathService.getAll());
    }

    /** Chi tiết 1 lộ trình */
    @GetMapping("/{id}")
    public ApiResponse<LearningPathResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(learningPathService.getById(id));
    }

    /** Tạo lộ trình mới (Mentor hoặc Admin) */
    @PostMapping
    public ApiResponse<LearningPathResponse> create(
            @RequestParam UUID mentorId,
            @RequestBody LearningPathCreateRequest request) {
        return ApiResponse.success(learningPathService.create(mentorId, request));
    }

    /** Admin duyệt */
    @PatchMapping("/{id}/approve")
    public ApiResponse<LearningPathResponse> approve(@PathVariable UUID id) {
        return ApiResponse.success(learningPathService.approve(id));
    }

    /** Admin từ chối */
    @PatchMapping("/{id}/reject")
    public ApiResponse<LearningPathResponse> reject(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "") String reason) {
        return ApiResponse.success(learningPathService.reject(id, reason));
    }

    /** User đăng ký học lộ trình */
    @PostMapping("/{id}/enroll")
    public ApiResponse<LearningPathEnrollResponse> enroll(@PathVariable UUID id) {
        return ApiResponse.success(learningPathService.enroll(id));
    }
}
