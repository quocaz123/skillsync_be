package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.Session;
import com.skillsync.skillsync.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    // For UserService stats (already used)
    long countByTeacherId(UUID teacherId);
    long countByLearnerId(UUID learnerId);

    // For /api/sessions/mine
    List<Session> findByLearnerIdOrderByCreatedAtDesc(UUID learnerId);
    List<Session> findByTeacherIdOrderByCreatedAtDesc(UUID teacherId);
    List<Session> findByLearnerIdAndStatusOrderByCreatedAtDesc(UUID learnerId, SessionStatus status);
    List<Session> findByTeacherIdAndStatusOrderByCreatedAtDesc(UUID teacherId, SessionStatus status);

    List<Session> findByStatusInOrderByCreatedAtDesc(List<SessionStatus> statuses);

    // Check if slot is already booked (used for booking constraint)
    boolean existsBySlotIdAndStatusNot(UUID slotId, SessionStatus status);

    Optional<Session> findByIdAndLearnerId(UUID id, UUID learnerId);
    Optional<Session> findByIdAndTeacherId(UUID id, UUID teacherId);

    List<Session> findByStatusAndEndedAtBefore(SessionStatus status, java.time.LocalDateTime time);
    
    // For approval logic
    List<Session> findBySlotIdAndStatus(UUID slotId, SessionStatus status);

    @Query("SELECT COALESCE(SUM(s.creditCost), 0) FROM Session s WHERE s.learner.id = :userId AND s.status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'DISPUTED')")
    Integer getLearnerPendingCredits(@Param("userId") UUID userId);

    @Query("SELECT COALESCE(SUM(s.creditCost), 0) FROM Session s WHERE s.teacher.id = :userId AND s.status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'DISPUTED')")
    Integer getTeacherPendingCredits(@Param("userId") UUID userId);

    List<Session> findByLearnerIdAndStatusNotOrderByCreatedAtDesc(UUID learnerId, SessionStatus status);

    /**
     * Lấy các session SCHEDULED theo ngày slot (để scheduler tính reminder).
     * Chỉ lấy session chưa gửi reminder và chưa start.
     */
    @Query("""
            SELECT s
            FROM Session s
            JOIN FETCH s.slot sl
            JOIN FETCH s.learner l
            JOIN FETCH s.teacher t
            WHERE s.status = com.skillsync.skillsync.enums.SessionStatus.SCHEDULED
              AND s.startedAt IS NULL
              AND s.reminderSentAt IS NULL
              AND sl.slotDate IN :dates
            """)
    List<Session> findScheduledSessionsForReminder(@Param("dates") List<LocalDate> dates);
}
