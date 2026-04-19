package com.skillsync.skillsync.entity;

import com.skillsync.skillsync.enums.LearningPathStatus;
import com.skillsync.skillsync.enums.RegistrationType;
import com.skillsync.skillsync.enums.SkillCategory;
import com.skillsync.skillsync.enums.SkillLevel;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "learning_paths")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LearningPath {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    User teacher;

    @Column(nullable = false, length = 200)
    String title;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "short_description", length = 500)
    String shortDescription;

    @Enumerated(EnumType.STRING)
    SkillCategory category;

    @Enumerated(EnumType.STRING)
    SkillLevel level;

    @Column(length = 100)
    String duration;

    @Column(length = 10)
    String emoji;

    @Column(name = "thumbnail_url", length = 500)
    String thumbnailUrl;

    @Column(name = "total_credits", nullable = false)
    @Builder.Default
    Integer totalCredits = 0;

    @Column(name = "max_students")
    @Builder.Default
    Integer maxStudents = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_type", nullable = false)
    @Builder.Default
    RegistrationType registrationType = RegistrationType.AUTO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    LearningPathStatus status = LearningPathStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    String rejectionReason;

    @OneToMany(mappedBy = "learningPath", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    List<LearningPathModule> modules = new ArrayList<>();

    @OneToMany(mappedBy = "learningPath", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    List<LearningPathEnrollment> enrollments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
