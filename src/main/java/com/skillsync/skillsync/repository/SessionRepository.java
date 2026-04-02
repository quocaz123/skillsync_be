package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.Session;
import com.skillsync.skillsync.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    // Check if slot is already booked
    boolean existsBySlotId(UUID slotId);

    Optional<Session> findByIdAndLearnerId(UUID id, UUID learnerId);
    Optional<Session> findByIdAndTeacherId(UUID id, UUID teacherId);

    List<Session> findByStatusAndEndedAtBefore(SessionStatus status, java.time.LocalDateTime time);
}
