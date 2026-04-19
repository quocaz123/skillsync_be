package com.skillsync.skillsync.service;

import com.skillsync.skillsync.entity.CreditTransaction;
import com.skillsync.skillsync.entity.Session;
import com.skillsync.skillsync.entity.SessionReport;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.enums.ReportReason;
import com.skillsync.skillsync.enums.ReportStatus;
import com.skillsync.skillsync.enums.SessionStatus;
import com.skillsync.skillsync.enums.TransactionType;
import com.skillsync.skillsync.exception.AppException;
import com.skillsync.skillsync.exception.ErrorCode;
import com.skillsync.skillsync.repository.CreditTransactionRepository;
import com.skillsync.skillsync.repository.SessionReportRepository;
import com.skillsync.skillsync.repository.SessionRepository;
import com.skillsync.skillsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionReportService {

    private final SessionReportRepository reportRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final CreditTransactionRepository transactionRepository;

    @Transactional
    public SessionReport createReport(UUID sessionId, ReportReason reason, String description, String evidenceUrl) {
        User user = userService.getCurrentUser();
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        boolean isLearner = session.getLearner().getId().equals(user.getId());
        boolean isTeacher = session.getTeacher().getId().equals(user.getId());

        if (!isLearner && !isTeacher) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        User reportedUser = isLearner ? session.getTeacher() : session.getLearner();

        // Check if money is already transferred. 
        // If money is already transferred (EARN_SESSION), still allow report for logging, but no escrow freeze
        boolean alreadyPaid = transactionRepository.existsByReferenceIdAndTransactionType(sessionId, TransactionType.EARN_SESSION);

        // Put session in DISPUTED state if not paid
        if (!alreadyPaid && session.getStatus() == SessionStatus.COMPLETED) {
            session.setStatus(SessionStatus.DISPUTED);
            sessionRepository.save(session);
        }

        SessionReport report = SessionReport.builder()
                .session(session)
                .reporter(user)
                .reportedUser(reportedUser)
                .reason(reason)
                .description(description)
                .evidenceUrl(evidenceUrl)
                .status(ReportStatus.PENDING)
                .build();

        return reportRepository.save(report);
    }

    public List<SessionReport> getPendingReports() {
        return reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING);
    }

    public List<SessionReport> getAllReports() {
        return reportRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    @Transactional
    public SessionReport resolveReport(UUID reportId, ReportStatus resolution, String adminNotes) {
        User admin = userService.getCurrentUser(); // Assume protected by Admin Role
        SessionReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Session session = report.getSession();
        boolean alreadyPaid = transactionRepository.existsByReferenceIdAndTransactionType(session.getId(), TransactionType.EARN_SESSION);
        boolean alreadyRefunded = transactionRepository.existsByReferenceIdAndTransactionType(session.getId(), TransactionType.REFUND);

        // Handle Escrow
        if (!alreadyPaid && !alreadyRefunded && session.getStatus() == SessionStatus.DISPUTED) {
            // RESOLVED_IN_FAVOR_OF_REPORTER
            if (resolution == ReportStatus.RESOLVED && report.getReporter().getId().equals(session.getLearner().getId())) {
                // Refund Learner
                User learner = session.getLearner();
                learner.setCreditsBalance((learner.getCreditsBalance() != null ? learner.getCreditsBalance() : 0) + session.getCreditCost());
                userRepository.save(learner);

                CreditTransaction tx = CreditTransaction.builder()
                        .user(learner)
                        .amount(session.getCreditCost())
                        .transactionType(TransactionType.REFUND)
                        .referenceId(session.getId())
                        .description("Refund due to dispute resolution for session " + session.getVideoRoomId())
                        .build();
                transactionRepository.save(tx);
                
                session.setStatus(SessionStatus.CANCELLED);
                sessionRepository.save(session);
            } 
            // RESOLVED_IN_FAVOR_OF_REPORTED or FALSE_ALARM (Mentor wins)
            else if (resolution == ReportStatus.REJECTED && report.getReporter().getId().equals(session.getLearner().getId())) {
                // Teacher gets paid
                User teacher = session.getTeacher();
                teacher.setCreditsBalance((teacher.getCreditsBalance() != null ? teacher.getCreditsBalance() : 0) + session.getCreditCost());
                userRepository.save(teacher);

                CreditTransaction tx = CreditTransaction.builder()
                        .user(teacher)
                        .amount(session.getCreditCost())
                        .transactionType(TransactionType.EARN_SESSION)
                        .referenceId(session.getId())
                        .description("Earned from session " + session.getVideoRoomId() + " after dispute")
                        .build();
                transactionRepository.save(tx);
                
                session.setStatus(SessionStatus.COMPLETED);
                sessionRepository.save(session);
            }
        }

        // Update Penalties based on Resolution
        if (resolution == ReportStatus.RESOLVED) {
            // Reporter wins, ReportedUser loses
            if (report.getReportedUser() != null) {
                User loser = report.getReportedUser();
                loser.setViolationCount(loser.getViolationCount() != null ? loser.getViolationCount() + 1 : 1);
                if (loser.getViolationCount() >= 3) {
                    loser.setStatus(com.skillsync.skillsync.enums.UserStatus.BANNED);
                }
                userRepository.save(loser);
            }
        } else if (resolution == ReportStatus.REJECTED) {
            // ReportedUser wins, Reporter loses (False alarm)
            User loser = report.getReporter();
            loser.setViolationCount(loser.getViolationCount() != null ? loser.getViolationCount() + 1 : 1);
            if (loser.getViolationCount() >= 3) {
                loser.setStatus(com.skillsync.skillsync.enums.UserStatus.BANNED);
            }
            userRepository.save(loser);
        }

        report.setStatus(resolution);
        report.setAdminNotes(adminNotes);
        report.setResolvedBy(admin);
        report.setResolvedAt(LocalDateTime.now());
        
        return reportRepository.save(report);
    }

    public SessionReport getReportBySessionId(UUID sessionId) {
        List<SessionReport> reports = reportRepository.findBySessionId(sessionId);
        if (reports.isEmpty()) {
            throw new AppException(ErrorCode.NOT_FOUND);
        }
        return reports.get(0); // Return the first/latest report
    }

    @Transactional
    public SessionReport submitCounterEvidence(UUID reportId, String description, String evidenceUrl) {
        User reportedUser = userService.getCurrentUser();
        SessionReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (report.getReportedUser() == null || !report.getReportedUser().getId().equals(reportedUser.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        report.setCounterDescription(description);
        report.setCounterEvidenceUrl(evidenceUrl);
        report.setCounterSubmittedAt(LocalDateTime.now());

        return reportRepository.save(report);
    }
}
