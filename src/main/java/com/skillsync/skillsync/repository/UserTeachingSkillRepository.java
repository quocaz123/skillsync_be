package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.UserTeachingSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserTeachingSkillRepository extends JpaRepository<UserTeachingSkill, UUID> {
}
