package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.common.ApiResponse;
import com.skillsync.skillsync.dto.response.mission.UserMissionResponse;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.service.UserMissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/user-missions")
@RequiredArgsConstructor
public class UserMissionController {

    private final UserMissionService userMissionService;

    @GetMapping("/me")
    public ApiResponse<List<UserMissionResponse>> getMyMissions(Authentication authentication) {
        return ApiResponse.success(userMissionService.getMyMissions(authentication.getName()));
    }

    @PostMapping("/{missionId}/complete")
    public ApiResponse<UserMissionResponse> completeMission(
            Authentication authentication,
            @PathVariable UUID missionId) {
        return ApiResponse.success(userMissionService.completeMission(authentication.getName(),
                missionId));
    }

    @PostMapping("/action/{action}")
    public ApiResponse<Void> trackAction(
            Authentication authentication,
            @PathVariable String action) {
        userMissionService.trackAction(authentication.getName(), action);
        return ApiResponse.success(null);
    }
}
