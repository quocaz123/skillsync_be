package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.UserTeachingSkill;
import com.skillsync.skillsync.entity.Skill;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.enums.SkillLevel;
import com.skillsync.skillsync.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface UserTeachingSkillRepository extends JpaRepository<UserTeachingSkill, UUID> {
    List<UserTeachingSkill> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Used by AdminTeachingSkillService
    List<UserTeachingSkill> findByVerificationStatusOrderByCreatedAtAsc(VerificationStatus status);
    List<UserTeachingSkill> findAllByOrderByCreatedAtDesc();

    // Used by some services (create/idempotent)
    Optional<UserTeachingSkill> findByUserAndSkillAndLevel(User user, Skill skill, SkillLevel level);

    boolean existsByUserIdAndSkillIdAndLevel(UUID userId, UUID skillId, com.skillsync.skillsync.enums.SkillLevel level);
}
