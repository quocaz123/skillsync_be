package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SystemLogRepository extends JpaRepository<SystemLog, UUID> {
    Page<SystemLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
