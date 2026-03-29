package com.skillsync.skillsync.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "user_missions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "mission_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserMission {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    CreditMission mission;

    @Builder.Default
    Integer progress = 0;

    @Builder.Default
    @Column(name = "is_completed")
    Boolean isCompleted = false;

    @Column(name = "reward_claimed_at")
    LocalDateTime rewardClaimedAt;

    @CreationTimestamp
    @Column(name = "created_at")
    LocalDateTime createdAt;
}
