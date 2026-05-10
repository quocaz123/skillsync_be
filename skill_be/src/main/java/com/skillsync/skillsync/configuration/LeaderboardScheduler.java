package com.skillsync.skillsync.configuration;

import com.skillsync.skillsync.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class LeaderboardScheduler {

    private final LeaderboardService leaderboardService;

    /**
     * Reset bảng xếp hạng vào 00:00 ngày đầu tiên của mỗi tháng.
     * Cron format: "0 0 0 1 * ?" (Giây Phút Giờ Ngày Tháng NgàyTrongTuần)
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    public void resetMonthlyLeaderboard() {
        log.info("📅 Starting monthly leaderboard reset process...");
        leaderboardService.clearLeaderboard();
        log.info("✅ Monthly leaderboard has been reset successfully.");
    }
}
