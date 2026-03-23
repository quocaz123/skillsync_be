package com.skillsync.skillsync.entity;

import com.skillsync.skillsync.enums.SkillLevel;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_teaching_skills")
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
    @JoinColumn(name = "user_id")
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    Skill skill;

    @Enumerated(EnumType.STRING)
    SkillLevel level;

    @Column(name = "experience_desc", columnDefinition = "TEXT")
    String experienceDesc;

    @Column(name = "outcome_desc", columnDefinition = "TEXT")
    String outcomeDesc;

    @Builder.Default
    @Column(name = "credits_per_hour")
    Integer creditsPerHour = 12;

    @CreationTimestamp
    @Column(name = "created_at")
    LocalDateTime createdAt;
}
