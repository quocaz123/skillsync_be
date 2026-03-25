package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.UserTeachingSkill;
import com.skillsync.skillsync.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserTeachingSkillRepository extends JpaRepository<UserTeachingSkill, UUID> {
    List<UserTeachingSkill> findByUserIdOrderByCreatedAtDesc(UUID userId);
    boolean existsByUserIdAndSkillIdAndLevel(UUID userId, UUID skillId, com.skillsync.skillsync.enums.SkillLevel level);
    List<UserTeachingSkill> findByVerificationStatusOrderByCreatedAtAsc(VerificationStatus status);
    List<UserTeachingSkill> findAllByOrderByCreatedAtDesc();
}


