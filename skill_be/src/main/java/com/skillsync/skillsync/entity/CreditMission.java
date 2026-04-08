package com.skillsync.skillsync.entity;

import com.skillsync.skillsync.enums.MissionStatus;
import com.skillsync.skillsync.enums.MissionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit_missions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreditMission {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false)
    String title;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "reward_amount", nullable = false)
    Integer rewardAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "mission_type")
    MissionType missionType;

    @Column(name = "target_action", length = 100)
    String targetAction;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    MissionStatus status = MissionStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at")
    LocalDateTime createdAt;
}
