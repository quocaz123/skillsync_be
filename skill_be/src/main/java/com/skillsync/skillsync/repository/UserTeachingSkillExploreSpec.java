package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.UserTeachingSkill;
import com.skillsync.skillsync.enums.SkillCategory;
import com.skillsync.skillsync.enums.VerificationStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Điều kiện lọc Explore công khai — một query, JOIN có chủ đích, tránh N+1 ở tầng lọc.
 */
public final class UserTeachingSkillExploreSpec {

    private UserTeachingSkillExploreSpec() {}

    public static Specification<UserTeachingSkill> approvedPublic(
            String q,
            UUID skillId,
            SkillCategory category
    ) {
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }

            List<Predicate> parts = new ArrayList<>();
            parts.add(cb.equal(root.get("verificationStatus"), VerificationStatus.APPROVED));
            parts.add(cb.isFalse(root.get("hidden")));

            Join<Object, Object> skillJoin = root.join("skill", JoinType.INNER);
            Join<Object, Object> userJoin = root.join("user", JoinType.INNER);

            if (skillId != null) {
                parts.add(cb.equal(skillJoin.get("id"), skillId));
            }
            if (category != null) {
                parts.add(cb.equal(skillJoin.get("category"), category));
            }
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                parts.add(cb.or(
                        cb.like(cb.lower(userJoin.get("fullName")), pattern),
                        cb.like(cb.lower(skillJoin.get("name")), pattern)
                ));
            }

            return cb.and(parts.toArray(Predicate[]::new));
        };
    }
}
