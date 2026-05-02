package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.common.ApiResponse;
import com.skillsync.skillsync.entity.CreditMission;
import com.skillsync.skillsync.enums.MissionStatus;
import com.skillsync.skillsync.repository.CreditMissionRepository;
import com.skillsync.skillsync.repository.UserMissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/missions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMissionController {

    private final CreditMissionRepository missionRepository;
    private final UserMissionRepository userMissionRepository;

    /** GET /api/admin/missions — danh sách missions kèm số lượng completions */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getAllMissions() {
        List<CreditMission> missions = missionRepository.findAll();

        List<Map<String, Object>> result = missions.stream()
                .sorted(Comparator.comparing(CreditMission::getCreatedAt).reversed())
                .map(m -> {
                    long completions = userMissionRepository.countByMissionId(m.getId());
                    long uniqueUsers = userMissionRepository.countDistinctUserByMissionId(m.getId());

                    Map<String, Object> dto = new LinkedHashMap<>();
                    dto.put("id", m.getId());
                    dto.put("title", m.getTitle());
                    dto.put("description", m.getDescription());
                    dto.put("rewardAmount", m.getRewardAmount());
                    dto.put("missionType", m.getMissionType());
                    dto.put("targetAction", m.getTargetAction());
                    dto.put("status", m.getStatus());
                    dto.put("isActive", m.getStatus() == MissionStatus.ACTIVE);
                    dto.put("createdAt", m.getCreatedAt());
                    dto.put("totalCompletions", completions);
                    dto.put("uniqueUsers", uniqueUsers);
                    dto.put("totalRewardsDistributed", completions * (m.getRewardAmount() != null ? m.getRewardAmount() : 0));
                    return dto;
                })
                .collect(Collectors.toList());

        return ApiResponse.success(result);
    }

    /** PATCH /api/admin/missions/{id}/toggle — bật/tắt active */
    @PatchMapping("/{id}/toggle")
    public ApiResponse<Map<String, Object>> toggleMission(@PathVariable UUID id) {
        CreditMission mission = missionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mission not found"));

        mission.setStatus(mission.getStatus() == MissionStatus.ACTIVE
                ? MissionStatus.INACTIVE
                : MissionStatus.ACTIVE);
        missionRepository.save(mission);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", mission.getId());
        res.put("status", mission.getStatus());
        res.put("isActive", mission.getStatus() == MissionStatus.ACTIVE);
        return ApiResponse.success(res);
    }
}
