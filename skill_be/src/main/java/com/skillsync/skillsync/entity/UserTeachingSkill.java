package com.skillsync.skillsync.entity;

import com.skillsync.skillsync.enums.SkillLevel;
import com.skillsync.skillsync.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_teaching_skills", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "skill_id",
        "level" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserTeachingSkill {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id", nullable = false)
    Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    SkillLevel level;

    @Column(name = "experience_desc", columnDefinition = "TEXT", nullable = false)
    String experienceDesc;

    @Column(name = "outcome_desc", columnDefinition = "TEXT", nullable = false)
    String outcomeDesc;

    @Column(name = "teaching_style", columnDefinition = "TEXT")
    String teachingStyle;

    @Builder.Default
    @Column(name = "credits_per_hour", nullable = false)
    Integer creditsPerHour = 12;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    User verifiedBy;

    @Column(name = "verified_at")
    LocalDateTime verifiedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at")
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}