package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.response.LeaderboardResponse;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.enums.Role;
import com.skillsync.skillsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;

    private static final String LEADERBOARD_KEY = "leaderboard:contribution";

    /**
     * Tăng điểm cống hiến cho user trong Redis.
     */
    public void incrementScore(String userId, double delta) {
        try {
            redisTemplate.opsForZSet().incrementScore(LEADERBOARD_KEY, userId, delta);
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật leaderboard cho user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Ghi đè điểm cống hiến (Dùng cho seeding hoặc reset).
     */
    public void setScore(String userId, double score) {
        try {
            redisTemplate.opsForZSet().add(LEADERBOARD_KEY, userId, score);
        } catch (Exception e) {
            log.error("Lỗi khi set điểm leaderboard cho user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Xóa toàn bộ bảng xếp hạng (Dùng cho reset tháng).
     */
    public void clearLeaderboard() {
        redisTemplate.delete(LEADERBOARD_KEY);
        log.info("🧹 Leaderboard has been reset for the new month.");
    }

    /**
     * Lấy danh sách bảng xếp hạng từ Redis.
     */
    public List<LeaderboardResponse> getTopUsers(int limit) {
        Set<ZSetOperations.TypedTuple<Object>> topUsers = redisTemplate.opsForZSet()
                .reverseRangeWithScores(LEADERBOARD_KEY, 0, limit - 1);

        if (topUsers == null || topUsers.isEmpty()) {
            return Collections.emptyList();
        }

        List<java.util.UUID> userIds = topUsers.stream()
                .map(tuple -> java.util.UUID.fromString(String.valueOf(tuple.getValue())))
                .collect(Collectors.toList());

        Map<java.util.UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<LeaderboardResponse> response = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<Object> tuple : topUsers) {
            String userIdStr = String.valueOf(tuple.getValue());
            java.util.UUID userId = java.util.UUID.fromString(userIdStr);
            Double score = tuple.getScore();
            User user = userMap.get(userId);

            if (user != null && user.getRole() != Role.ADMIN) {
                response.add(LeaderboardResponse.builder()
                        .userId(userIdStr)
                        .name(user.getFullName())
                        .avatarUrl(user.getAvatarUrl())
                        .avatarGrad(getMockAvatarGrad(rank)) 
                        .score(score)
                        .rankTier(calculateTier(score))
                        .position(rank++)
                        .build());
            }
        }
        return response;
    }

    private String calculateTier(Double score) {
        if (score == null) return "Bronze";
        if (score >= 100000) return "Vanguard";
        if (score >= 50000) return "Diamond";
        if (score >= 20000) return "Platinum";
        if (score >= 5000) return "Gold";
        if (score >= 1000) return "Silver";
        return "Bronze";
    }

    private String getMockAvatarGrad(int rank) {
        if (rank == 1) return "from-amber-400 to-orange-500";
        if (rank <= 3) return "from-sky-400 to-blue-500";
        return "from-slate-300 to-slate-400";
    }
}
