package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    boolean existsBySessionIdAndReviewerId(UUID sessionId, UUID reviewerId);

    List<Review> findByRevieweeIdOrderByCreatedAtDesc(UUID revieweeId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewee.id = :userId")
    Double findAverageRatingByRevieweeId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.reviewee.id = :userId")
    Long countByRevieweeId(@Param("userId") UUID userId);

    @Query("SELECT r FROM Review r WHERE r.session.teachingSkill.id IN :skillIds " +
           "AND r.reviewer.id = r.session.learner.id " +
           "ORDER BY r.createdAt DESC")
    List<Review> findLearnerReviewsByTeachingSkillIds(@Param("skillIds") List<UUID> skillIds);
}
