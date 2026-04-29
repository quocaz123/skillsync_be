package com.skillsync.skillsync.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Table(name = "learning_path_lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LearningPathLesson {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    LearningPathModule module;

    @Column(nullable = false, length = 200)
    String title;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "video_url", length = 500)
    String videoUrl;

    @Column(name = "duration_minutes")
    Integer durationMinutes;

    @Column(name = "is_preview")
    @Builder.Default
    Boolean isPreview = false;

    @Column(name = "order_index")
    @Builder.Default
    Integer orderIndex = 0;
}
