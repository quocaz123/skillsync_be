package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.LearningPathEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface LearningPathEnrollmentRepository extends JpaRepository<LearningPathEnrollment, UUID> {
    boolean existsByLearningPathIdAndStudentId(UUID learningPathId, UUID studentId);
    Optional<LearningPathEnrollment> findByLearningPathIdAndStudentId(UUID learningPathId, UUID studentId);
    List<LearningPathEnrollment> findByStudentIdOrderByEnrolledAtDesc(UUID studentId);
}
