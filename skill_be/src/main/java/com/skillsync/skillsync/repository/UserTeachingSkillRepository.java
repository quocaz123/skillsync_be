package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.UserTeachingSkill;
import com.skillsync.skillsync.entity.Skill;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.enums.SkillLevel;
import com.skillsync.skillsync.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Explore sort "experience": phân trang SQL — số buỗi đã book (BOOKED slot), không load toàn bộ bảng.
     * Điều kiện lọc khớp {@link UserTeachingSkillExploreSpec#approvedPublic}.
     */
    @Query(value = """
            SELECT uts.id FROM user_teaching_skills uts
            INNER JOIN users u ON u.id = uts.user_id
            INNER JOIN skills sk ON sk.id = uts.skill_id
            LEFT JOIN (
              SELECT ts.teaching_skill_id AS tid,
                     SUM(CASE WHEN ts.status = 'BOOKED' THEN 1 ELSE 0 END) AS booked_cnt
              FROM teaching_slots ts
              GROUP BY ts.teaching_skill_id
            ) agg ON agg.tid = uts.id
            WHERE uts.verification_status = 'APPROVED'
              AND uts.is_hidden = false
              AND ((NOT :filterSkill) OR uts.skill_id = :skillId)
              AND ((NOT :filterCategory) OR sk.category = :categoryStr)
              AND ((NOT :filterQ) OR (
                    POSITION(:qPlain IN LOWER(u.full_name)) > 0
                 OR POSITION(:qPlain IN LOWER(sk.name)) > 0))
            ORDER BY COALESCE(agg.booked_cnt, 0) DESC, uts.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(uts.id) FROM user_teaching_skills uts
            INNER JOIN users u ON u.id = uts.user_id
            INNER JOIN skills sk ON sk.id = uts.skill_id
            WHERE uts.verification_status = 'APPROVED'
              AND uts.is_hidden = false
              AND ((NOT :filterSkill) OR uts.skill_id = :skillId)
              AND ((NOT :filterCategory) OR sk.category = :categoryStr)
              AND ((NOT :filterQ) OR (
                    POSITION(:qPlain IN LOWER(u.full_name)) > 0
                 OR POSITION(:qPlain IN LOWER(sk.name)) > 0))
            """,
            nativeQuery = true)
    Page<UUID> findExploreIdsOrderByExperience(
            @Param("filterSkill") boolean filterSkill,
            @Param("skillId") UUID skillId,
            @Param("filterCategory") boolean filterCategory,
            @Param("categoryStr") String categoryStr,
            @Param("filterQ") boolean filterQ,
            @Param("qPlain") String qPlain,
            Pageable pageable);
}
