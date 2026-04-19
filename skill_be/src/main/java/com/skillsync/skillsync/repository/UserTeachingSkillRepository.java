package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.UserTeachingSkill;
import com.skillsync.skillsync.entity.Skill;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.enums.SkillLevel;
import com.skillsync.skillsync.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface UserTeachingSkillRepository extends JpaRepository<UserTeachingSkill, UUID>, JpaSpecificationExecutor<UserTeachingSkill> {
    List<UserTeachingSkill> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Used by AdminTeachingSkillService
    List<UserTeachingSkill> findByVerificationStatusOrderByCreatedAtAsc(VerificationStatus status);
    List<UserTeachingSkill> findAllByOrderByCreatedAtDesc();
    List<UserTeachingSkill> findByVerificationStatus(VerificationStatus status);

    /** Explore / công khai — chỉ skill đã duyệt và mentor chưa tạm ẩn */
    List<UserTeachingSkill> findByVerificationStatusAndHiddenFalse(VerificationStatus status);

    // Used by some services (create/idempotent)
    Optional<UserTeachingSkill> findByUserAndSkillAndLevel(User user, Skill skill, SkillLevel level);

    boolean existsByUserIdAndSkillIdAndLevel(UUID userId, UUID skillId, com.skillsync.skillsync.enums.SkillLevel level);

    @Query("""
            SELECT uts FROM UserTeachingSkill uts
            JOIN FETCH uts.user u
            JOIN FETCH uts.skill s
            WHERE uts.verificationStatus = :status
            AND uts.hidden = false
            AND LOWER(s.name) IN :skillNames
            ORDER BY uts.createdAt DESC
            """)
    List<UserTeachingSkill> findApprovedBySkillNames(@Param("skillNames") List<String> skillNames,
                                                     @Param("status") VerificationStatus status);
}
