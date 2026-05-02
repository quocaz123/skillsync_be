package com.skillsync.skillsync.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "learning_path_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LearningPathReview {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_path_id", nullable = false)
    LearningPath learningPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    User reviewer;

    @Column(name = "reviewer_id", insertable = false, updatable = false)
    UUID reviewerId;

    @Column(name = "learning_path_id", insertable = false, updatable = false)
    UUID learningPathId;

    @Column(nullable = false)
    Integer rating;

    @Column(columnDefinition = "TEXT")
    String comment;

    @Column(name = "tags", columnDefinition = "TEXT")
    String tags; // Stored as comma-separated values

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}
