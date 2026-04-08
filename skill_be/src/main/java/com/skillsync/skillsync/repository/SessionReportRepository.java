package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.SessionReport;
import com.skillsync.skillsync.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SessionReportRepository extends JpaRepository<SessionReport, UUID> {
    List<SessionReport> findByStatusOrderByCreatedAtDesc(ReportStatus status);
    List<SessionReport> findBySessionId(UUID sessionId);
}
