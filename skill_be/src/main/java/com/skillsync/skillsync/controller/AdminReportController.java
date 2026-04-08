package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.common.ApiResponse;
import com.skillsync.skillsync.dto.request.report.ResolveReportRequest;
import com.skillsync.skillsync.dto.response.report.ReportResponse;
import com.skillsync.skillsync.entity.SessionReport;
import com.skillsync.skillsync.service.SessionReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final SessionReportService reportService;

    @GetMapping
    public ApiResponse<List<ReportResponse>> getAllReports() {
        List<SessionReport> reports = reportService.getAllReports();
        return ApiResponse.success(reports.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @PatchMapping("/{reportId}/resolve")
    public ApiResponse<ReportResponse> resolveReport(
            @PathVariable UUID reportId,
            @RequestBody ResolveReportRequest request) {
        SessionReport report = reportService.resolveReport(reportId, request.getResolution(), request.getAdminNotes());
        return ApiResponse.success(toResponse(report));
    }

    private ReportResponse toResponse(SessionReport report) {
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
