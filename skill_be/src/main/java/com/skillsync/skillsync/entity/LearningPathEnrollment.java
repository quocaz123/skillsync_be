package com.skillsync.skillsync.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "learning_path_enrollments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"learning_path_id", "student_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LearningPathEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_path_id", nullable = false)
    LearningPath learningPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    User student;

    @Column(name = "learner_id", nullable = false)
    UUID learnerId;

    @Column(name = "progress_percent", nullable = false)
    @Builder.Default
    Integer progressPercent = 0;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    String status = "ENROLLED";

    @CreationTimestamp
    @Column(name = "enrolled_at", updatable = false)
    LocalDateTime enrolledAt;
}
