package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.UserMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface UserMissionRepository extends JpaRepository<UserMission, UUID> {
    Optional<UserMission> findByUserIdAndMissionId(UUID userId, UUID missionId);
    List<UserMission> findAllByUserId(UUID userId);
}
