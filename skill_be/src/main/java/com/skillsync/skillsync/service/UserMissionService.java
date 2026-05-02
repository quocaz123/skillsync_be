package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.response.mission.UserMissionResponse;
import com.skillsync.skillsync.entity.CreditMission;
import com.skillsync.skillsync.entity.CreditTransaction;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.exception.AppException;
import com.skillsync.skillsync.exception.ErrorCode;
import com.skillsync.skillsync.entity.UserMission;
import com.skillsync.skillsync.enums.MissionType;
import com.skillsync.skillsync.enums.TransactionType;
import com.skillsync.skillsync.repository.CreditMissionRepository;
import com.skillsync.skillsync.repository.CreditTransactionRepository;
import com.skillsync.skillsync.repository.UserMissionRepository;
import com.skillsync.skillsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserMissionService {

    private final CreditMissionRepository creditMissionRepository;
    private final UserMissionRepository userMissionRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final UserRepository userRepository;

    public List<UserMissionResponse> getMyMissions(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<CreditMission> allMissions = creditMissionRepository.findByStatus(com.skillsync.skillsync.enums.MissionStatus.ACTIVE);
        List<UserMission> userMissions = userMissionRepository.findAllByUserId(user.getId());

        return allMissions.stream().map(mission -> {
            UserMission um = userMissions.stream()
                    .filter(m -> m.getMission().getId().equals(mission.getId()))
                    .findFirst()
                    .orElse(null);

            String status = "pending";
            if (um != null) {
                if (um.getIsCompleted()) {
                    if (mission.getMissionType() == MissionType.DAILY) {
                        if (um.getRewardClaimedAt() != null
                                && um.getRewardClaimedAt().toLocalDate().equals(LocalDate.now())) {
                            status = "completed";
                        }
                    } else {
                        status = "completed";
                    }
                } else if (um.getProgress() != null && um.getProgress() > 0) {
                    status = "in-progress";
                }
            }

            // Auto complete LOGIN progress if pending
            if (mission.getTargetAction().equals("LOGIN") && status.equals("pending")) {
                if (um == null) {
                    um = UserMission.builder().user(user).mission(mission).progress(1).isCompleted(false).build();
                    userMissionRepository.save(um);
                } else {
                    um.setProgress(1);
                    userMissionRepository.save(um);
                }
                status = "in-progress";
            }

            return UserMissionResponse.builder()
                    .id(mission.getId().toString())
                    .title(mission.getTitle())
                    .description(mission.getDescription())
                    .reward(mission.getRewardAmount())
                    .type(mission.getMissionType() == MissionType.DAILY ? "daily" : "once")
                    .status(status)
                    .targetAction(mission.getTargetAction())
                    .build();
        }).toList();
    }

    @Transactional
    public UserMissionResponse completeMission(String email, UUID missionId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        CreditMission mission = creditMissionRepository.findById(missionId)
                .orElseThrow(() -> new IllegalArgumentException("Mission not found"));

        UserMission um = userMissionRepository.findByUserIdAndMissionId(user.getId(), missionId)
                .orElseGet(() -> UserMission.builder()
                        .user(user)
                        .mission(mission)
                        .progress(0)
                        .isCompleted(false)
                        .build());

        if (um.getIsCompleted()) {
            if (mission.getMissionType() == MissionType.ONCE) {
                throw new AppException(ErrorCode.MISSION_ALREADY_COMPLETED, "Mission already completed");
            } else if (mission.getMissionType() == MissionType.DAILY) {
                if (um.getRewardClaimedAt() != null && um.getRewardClaimedAt().toLocalDate().equals(LocalDate.now())) {
                    throw new AppException(ErrorCode.MISSION_ALREADY_COMPLETED, "Daily mission already completed today");
                }
            }
        }

        if (!um.getIsCompleted() && (um.getProgress() == null || um.getProgress() < 1)) {
            // Check if it's the 30-min online task, FE just calls complete blindly, but
            // backend should ideally rely on trackAction
            // We allow frontend to skip trackAction for ONLINE_30_MINS and just call
            // complete, but to be strict, we'll enforce it.
            // The FE must call trackAction first or we can bypass for ONLINE_30_MINS as FE
            // doesn't do multiple POSTs easily for time.
            if (!mission.getTargetAction().equals("ONLINE_30_MINS")) {
                throw new AppException(ErrorCode.MISSION_REQUIREMENTS_NOT_MET, "Chưa hoàn thành yêu cầu của nhiệm vụ.");
            }
        }

        um.setIsCompleted(true);
        um.setRewardClaimedAt(java.time.LocalDateTime.now());
        userMissionRepository.save(um);

        user.setCreditsBalance(user.getCreditsBalance() + mission.getRewardAmount());
        userRepository.save(user);

        CreditTransaction tx = CreditTransaction.builder()
                .user(user)
                .amount(mission.getRewardAmount())
                .transactionType(TransactionType.MISSION_REWARD)
                .description("Thưởng nhiệm vụ: " + mission.getTitle())
                .build();
        creditTransactionRepository.save(tx);

        return UserMissionResponse.builder()
                .id(mission.getId().toString())
                .title(mission.getTitle())
                .description(mission.getDescription())
                .reward(mission.getRewardAmount())
                .type(mission.getMissionType() == MissionType.DAILY ? "daily" : "once")
                .status("completed")
                .targetAction(mission.getTargetAction())
                .build();
    }

    @Transactional
    public void trackAction(String email, String action) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<CreditMission> missions = creditMissionRepository.findByTargetActionAndStatus(action, com.skillsync.skillsync.enums.MissionStatus.ACTIVE);
        for (CreditMission m : missions) {
            UserMission um = userMissionRepository.findByUserIdAndMissionId(user.getId(), m.getId())
                    .orElseGet(
                            () -> UserMission.builder().user(user).mission(m).progress(0).isCompleted(false).build());

            if (m.getMissionType() == MissionType.DAILY) {
                if (um.getRewardClaimedAt() != null && um.getRewardClaimedAt().toLocalDate().equals(LocalDate.now())) {
                    continue; // Already claimed today
                }
            } else {
                if (um.getIsCompleted())
                    continue;
            }

            um.setProgress(1); // Mặc định chỉ cần 1 lần thực hiện cho các nhiệm vụ hiện tại
            userMissionRepository.save(um);
        }
    }
}
