package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.common.ApiResponse;
import com.skillsync.skillsync.dto.request.forum.VerifyForumPostRequest;
import com.skillsync.skillsync.dto.response.forum.AdminForumPostResponse;
import com.skillsync.skillsync.enums.ForumPostStatus;
import com.skillsync.skillsync.enums.LogLevel;
import com.skillsync.skillsync.service.ForumPostService;
import com.skillsync.skillsync.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/forum-posts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminForumPostController {

    private final ForumPostService postService;
    private final SystemLogService systemLogService;

    @GetMapping
    public ApiResponse<List<AdminForumPostResponse>> getAll(
            @RequestParam(required = false) ForumPostStatus status
    ) {
        return ApiResponse.success(postService.getAdminPosts(status));
    }

    @PatchMapping("/{id}/verify")
    public ApiResponse<AdminForumPostResponse> verify(
            @PathVariable UUID id,
            @RequestBody VerifyForumPostRequest request
    ) {
        AdminForumPostResponse response = postService.verifyPost(id, request);
        systemLogService.logSystemEvent(
                "Duyet bai viet cong dong: " + request.getAction()
                        + " | postId=" + response.getId()
                        + " | title=" + response.getTitle(),
                LogLevel.INFO
        );
        return ApiResponse.success(response);
    }
}