package com.skillsync.skillsync.dto.response.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStatsResponse {
    private long totalUsers;
    private long totalSessions;
    private long activeSessions;       // SCHEDULED
    private long completedSessions;    // COMPLETED
    private long cancelledSessions;    // CANCELLED
    private long disputedSessions;     // DISPUTED
    private long pendingSkills;        // UserTeachingSkill PENDING
    private long pendingReports;       // SessionReport PENDING
    private long pendingForumPosts;    // ForumPost PENDING
    private long totalTransactions;
    private long escrowedCredits;      // sum creditCost where SCHEDULED/IN_PROGRESS/DISPUTED
}
