package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.common.ApiResponse;
import com.skillsync.skillsync.dto.response.session.SessionResponse;
import com.skillsync.skillsync.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.skillsync.skillsync.dto.request.admin.ResolveEscrowRequest;
import com.skillsync.skillsync.dto.response.report.ReportResponse;
import com.skillsync.skillsync.entity.SessionReport;
import com.skillsync.skillsync.service.SessionReportService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/escrow")
@RequiredArgsConstructor
public class AdminEscrowController {

    private final SessionService sessionService;
    private final SessionReportService sessionReportService;

    @GetMapping
    public ApiResponse<List<SessionResponse>> getEscrowSessions() {
        return ApiResponse.success(sessionService.getEscrowSessions());
    }

    @GetMapping("/{sessionId}/report")
    public ApiResponse<ReportResponse> getSessionReport(@PathVariable UUID sessionId) {
        SessionReport report = sessionReportService.getReportBySessionId(sessionId);
        return ApiResponse.success(toReportResponse(report));
    }

    @PostMapping("/{sessionId}/refund")
    public ApiResponse<Void> refundLearner(
            @PathVariable UUID sessionId,
            @RequestBody ResolveEscrowRequest request) {
        sessionService.resolveDisputeRefundLearner(sessionId, request.getAdminNotes());
        return ApiResponse.success(null);
    }

    @PostMapping("/{sessionId}/release")
    public ApiResponse<Void> releaseToMentor(
            @PathVariable UUID sessionId,
            @RequestBody ResolveEscrowRequest request) {
        sessionService.resolveDisputeReleaseToMentor(sessionId, request.getAdminNotes());
        return ApiResponse.success(null);
    }

    private ReportResponse toReportResponse(SessionReport report) {
        return ReportResponse.builder()
                .id(report.getId())
                .sessionId(report.getSession().getId())
                .reporterId(report.getReporter().getId())
                .reporterName(report.getReporter().getFullName())
                .reportedUserId(report.getReportedUser() != null ? report.getReportedUser().getId() : null)
                .reportedUserName(report.getReportedUser() != null ? report.getReportedUser().getFullName() : null)
                .reason(report.getReason())
                .description(report.getDescription())
                .evidenceUrl(report.getEvidenceUrl())
                .status(report.getStatus())
                .adminNotes(report.getAdminNotes())
                .resolvedAt(report.getResolvedAt())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
