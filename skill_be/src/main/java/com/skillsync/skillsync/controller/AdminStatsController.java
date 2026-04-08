package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.common.ApiResponse;
import com.skillsync.skillsync.dto.response.admin.AdminStatsResponse;
import com.skillsync.skillsync.enums.ForumPostStatus;
import com.skillsync.skillsync.enums.ReportStatus;
import com.skillsync.skillsync.enums.SessionStatus;
import com.skillsync.skillsync.enums.VerificationStatus;
import com.skillsync.skillsync.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final UserTeachingSkillRepository teachingSkillRepository;
    private final SessionReportRepository reportRepository;
    private final ForumPostRepository forumPostRepository;
    private final CreditTransactionRepository creditTransactionRepository;

    @GetMapping
    public ApiResponse<AdminStatsResponse> getStats() {
        long totalSessions = sessionRepository.count();
        long activeSessions = sessionRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(SessionStatus.SCHEDULED)).size();
        long completedSessions = sessionRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(SessionStatus.COMPLETED)).size();
        long cancelledSessions = sessionRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(SessionStatus.CANCELLED)).size();
        long disputedSessions = sessionRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(SessionStatus.DISPUTED)).size();

        // Escrow: sum creditCost for sessions in escrow states
        List<com.skillsync.skillsync.entity.Session> escrowSessions = sessionRepository
                .findByStatusInOrderByCreatedAtDesc(List.of(
                        SessionStatus.SCHEDULED,
                        SessionStatus.IN_PROGRESS,
                        SessionStatus.DISPUTED));
        long escrowedCredits = escrowSessions.stream()
                .mapToLong(s -> s.getCreditCost() != null ? s.getCreditCost() : 0)
                .sum();

        long pendingSkills = teachingSkillRepository
                .findByVerificationStatus(VerificationStatus.PENDING).size();

        long pendingReports = reportRepository
                .findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING).size();

        long pendingForumPosts = forumPostRepository
                .findByStatusOrderByCreatedAtDesc(ForumPostStatus.PENDING).size();

        AdminStatsResponse stats = AdminStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalSessions(totalSessions)
                .activeSessions(activeSessions)
                .completedSessions(completedSessions)
                .cancelledSessions(cancelledSessions)
                .disputedSessions(disputedSessions)
                .pendingSkills(pendingSkills)
                .pendingReports(pendingReports)
                .pendingForumPosts(pendingForumPosts)
                .totalTransactions(creditTransactionRepository.count())
                .escrowedCredits(escrowedCredits)
                .build();

        return ApiResponse.success(stats);
    }
}
