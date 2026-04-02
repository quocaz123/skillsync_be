package com.skillsync.skillsync.scheduler;

import com.skillsync.skillsync.entity.CreditTransaction;
import com.skillsync.skillsync.entity.Session;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.enums.SessionStatus;
import com.skillsync.skillsync.enums.TransactionType;
import com.skillsync.skillsync.repository.CreditTransactionRepository;
import com.skillsync.skillsync.repository.SessionRepository;
import com.skillsync.skillsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionScheduler {

    private final SessionRepository sessionRepository;
    private final CreditTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    /**
     * Executes every hour. Finds COMPLETED sessions > 48 hours ago
     * and releases the Escrow credits to the Teacher.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void autoConfirmSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(48);
        List<Session> completedSessions = sessionRepository.findByStatusAndEndedAtBefore(SessionStatus.COMPLETED, threshold);

        int count = 0;
        for (Session session : completedSessions) {
            boolean alreadyPaid = transactionRepository.existsByReferenceIdAndTransactionType(session.getId(), TransactionType.EARN_SESSION);
            boolean alreadyRefunded = transactionRepository.existsByReferenceIdAndTransactionType(session.getId(), TransactionType.REFUND);
            if (!alreadyPaid && !alreadyRefunded) {
                User teacher = session.getTeacher();
                teacher.setCreditsBalance((teacher.getCreditsBalance() != null ? teacher.getCreditsBalance() : 0) + session.getCreditCost());
                userRepository.save(teacher);

                CreditTransaction tx = CreditTransaction.builder()
                        .user(teacher)
                        .amount(session.getCreditCost())
                        .transactionType(TransactionType.EARN_SESSION)
                        .referenceId(session.getId())
                        .description("Auto-earned from session " + session.getVideoRoomId() + " after 48h")
                        .build();
                transactionRepository.save(tx);
                count++;
            }
        }
        if (count > 0) {
            log.info("Auto-confirmed {} sessions to transfer credits to mentors.", count);
        }
    }
}
