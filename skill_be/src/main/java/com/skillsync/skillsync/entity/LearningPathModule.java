package com.skillsync.skillsync.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "learning_path_modules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LearningPathModule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_path_id", nullable = false)
    LearningPath learningPath;

    @Column(nullable = false, length = 200)
    String title;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(length = 200)
    String objective;

    @Column(name = "order_index")
    @Builder.Default
    Integer orderIndex = 0;

    @Column(name = "enable_support")
    @Builder.Default
    Boolean enableSupport = false;

    @Column(name = "has_quiz")
    @Builder.Default
    Boolean hasQuiz = false;

    @Column(name = "is_quiz_mandatory")
    @Builder.Default
    Boolean isQuizMandatory = false;

    @Column(name = "sessions_needed", nullable = false)
    @Builder.Default
    Integer sessionsNeeded = 0;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)

    @Builder.Default
    List<LearningPathLesson> lessons = new ArrayList<>();
}
