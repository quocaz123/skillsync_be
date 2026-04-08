package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.Skill;
import com.skillsync.skillsync.enums.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {
    boolean existsByName(String name);
    List<Skill> findByCategory(SkillCategory category);
}

