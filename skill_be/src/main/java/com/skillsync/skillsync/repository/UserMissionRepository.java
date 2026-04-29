package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.UserMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface UserMissionRepository extends JpaRepository<UserMission, UUID> {
    Optional<UserMission> findByUserIdAndMissionId(UUID userId, UUID missionId);
    List<UserMission> findAllByUserId(UUID userId);

    /** Tổng số lần hoàn thành mission */
    long countByMissionId(UUID missionId);

    /** Số user riêng biệt đã hoàn thành mission */
    @Query("SELECT COUNT(DISTINCT um.user.id) FROM UserMission um WHERE um.mission.id = :missionId")
    long countDistinctUserByMissionId(UUID missionId);
}

