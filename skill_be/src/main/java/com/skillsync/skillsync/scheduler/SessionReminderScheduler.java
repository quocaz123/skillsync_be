package com.skillsync.skillsync.scheduler;

import com.skillsync.skillsync.dto.request.notification.NotificationCreateRequest;
import com.skillsync.skillsync.entity.Session;
import com.skillsync.skillsync.enums.NotificationType;
import com.skillsync.skillsync.enums.SessionStatus;
import com.skillsync.skillsync.repository.SessionRepository;
import com.skillsync.skillsync.service.NotificationService;
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
public class SessionReminderScheduler {

    private final SessionRepository sessionRepository;
    private final NotificationService notificationService;

    // Run every 5 minutes
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void sendSessionReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyMinutesLater = now.plusMinutes(30);
        LocalDateTime thirtyFiveMinutesLater = thirtyMinutesLater.plusMinutes(5);

        // Find scheduled sessions that are about to start in ~30 mins
        List<Session> upcomingSessions = sessionRepository.findByStatusInOrderByCreatedAtDesc(List.of(SessionStatus.SCHEDULED));

        for (Session session : upcomingSessions) {
            LocalDateTime slotTime = LocalDateTime.of(session.getSlot().getSlotDate(), session.getSlot().getSlotTime());

            if (slotTime.isAfter(thirtyMinutesLater) && slotTime.isBefore(thirtyFiveMinutesLater)) {
                // Notify Learner
                notificationService.createAndSend(NotificationCreateRequest.builder()
                        .userId(session.getLearner().getId())
                        .type(NotificationType.SESSION_REMINDER)
                        .title("Nhắc nhở buổi học")
                        .content("Buổi học với " + session.getTeacher().getFullName() + " sẽ bắt đầu trong 30 phút nữa.")
                        .entityId(session.getId())
                        .redirectUrl("/app/sessions")
                        .build());

                // Notify Teacher
                notificationService.createAndSend(NotificationCreateRequest.builder()
                        .userId(session.getTeacher().getId())
                        .type(NotificationType.SESSION_REMINDER)
                        .title("Nhắc nhở buổi dạy")
                        .content("Buổi dạy với " + session.getLearner().getFullName() + " sẽ bắt đầu trong 30 phút nữa.")
                        .entityId(session.getId())
                        .redirectUrl("/app/teaching")
                        .build());
                
                log.info("Sent reminders for session {}", session.getId());
            }
        }
    }
}
