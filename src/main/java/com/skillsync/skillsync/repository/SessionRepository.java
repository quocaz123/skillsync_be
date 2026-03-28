package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {
    long countByTeacherId(UUID teacherId);
    long countByLearnerId(UUID learnerId);
}
