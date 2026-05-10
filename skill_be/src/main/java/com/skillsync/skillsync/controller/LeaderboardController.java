package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.common.ApiResponse;
import com.skillsync.skillsync.dto.response.LeaderboardResponse;
import com.skillsync.skillsync.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping
    public ApiResponse<List<LeaderboardResponse>> getLeaderboard(@RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(leaderboardService.getTopUsers(limit));
    }
}
