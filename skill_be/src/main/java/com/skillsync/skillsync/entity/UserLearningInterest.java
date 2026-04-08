package com.skillsync.skillsync.entity;

import com.skillsync.skillsync.enums.SkillLevel;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_learning_interests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserLearningInterest {
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
    @Column(name = "desired_level")
    SkillLevel desiredLevel;

    @Column(name = "learning_goal", columnDefinition = "TEXT")
    String learningGoal;

    @CreationTimestamp
    @Column(name = "created_at")
    LocalDateTime createdAt;
}
