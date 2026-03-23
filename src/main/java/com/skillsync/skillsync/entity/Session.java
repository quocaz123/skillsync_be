package com.skillsync.skillsync.entity;

import com.skillsync.skillsync.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id")
    User learner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    User teacher;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id")
    TeachingSlot slot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teaching_skill_id")
    UserTeachingSkill teachingSkill;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    SessionStatus status = SessionStatus.SCHEDULED;

    @Column(name = "credit_cost", nullable = false)
    Integer creditCost;

    @Column(name = "jitsi_room_id", length = 100, unique = true)
    String jitsiRoomId;

    @Column(name = "jitsi_room_url", length = 500)
    String jitsiRoomUrl;

    @Column(name = "learner_notes", columnDefinition = "TEXT")
    String learnerNotes;

    @CreationTimestamp
    @Column(name = "created_at")
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
