package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.LearningPath;
import com.skillsync.skillsync.enums.LearningPathStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LearningPathRepository extends JpaRepository<LearningPath, UUID> {

    List<LearningPath> findByStatusOrderByCreatedAtDesc(LearningPathStatus status);

    List<LearningPath> findAllByOrderByCreatedAtDesc();

    List<LearningPath> findByTeacherIdOrderByCreatedAtDesc(UUID teacherId);

    @Query("SELECT DISTINCT lp FROM LearningPath lp JOIN lp.enrollments e WHERE e.student.id = :studentId ORDER BY lp.createdAt DESC")
    List<LearningPath> findEnrolledByStudentId(@Param("studentId") UUID studentId);

    @Query("SELECT lp FROM LearningPath lp WHERE lp.status = 'APPROVED' " +
           "AND (:category IS NULL OR lp.category = :category) " +
           "AND (:level IS NULL OR lp.level = :level) " +
           "ORDER BY lp.createdAt DESC")
    List<LearningPath> findApproved(
            @Param("category") String category,
            @Param("level") String level
    );
}
