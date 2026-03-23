package com.skillsync.skillsync.entity;

import com.skillsync.skillsync.enums.SlotStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "teaching_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeachingSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    User teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teaching_skill_id")
    UserTeachingSkill teachingSkill;

    @Column(name = "slot_date", nullable = false)
    LocalDate slotDate;

    @Column(name = "slot_time", nullable = false)
    LocalTime slotTime;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    SlotStatus status = SlotStatus.OPEN;

    @CreationTimestamp
    @Column(name = "created_at")
    LocalDateTime createdAt;
}
