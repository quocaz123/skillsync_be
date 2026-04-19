package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.TeachingSkillEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TeachingSkillEvidenceRepository extends JpaRepository<TeachingSkillEvidence, UUID> {
    List<TeachingSkillEvidence> findByTeachingSkillId(UUID teachingSkillId);
    List<TeachingSkillEvidence> findByTeachingSkillIdIn(List<UUID> skillIds);
    void deleteByTeachingSkillId(UUID teachingSkillId);
}
