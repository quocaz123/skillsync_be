package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.CreditMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CreditMissionRepository extends JpaRepository<CreditMission, UUID> {
    List<CreditMission> findByTargetAction(String targetAction);
}
