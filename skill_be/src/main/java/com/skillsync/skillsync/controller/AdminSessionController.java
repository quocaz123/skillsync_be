package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.common.ApiResponse;
import com.skillsync.skillsync.dto.response.session.SessionResponse;
import com.skillsync.skillsync.enums.SessionStatus;
import com.skillsync.skillsync.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/sessions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSessionController {

    private final SessionService sessionService;

    /**
     * GET /api/admin/sessions?status=SCHEDULED
     * Lấy tất cả sessions trên hệ thống, có thể lọc theo status.
     */
    @GetMapping
    public ApiResponse<List<SessionResponse>> getAllSessions(
            @RequestParam(required = false) SessionStatus status) {
        return ApiResponse.success(sessionService.getAllSessionsForAdmin(status));
    }
}
