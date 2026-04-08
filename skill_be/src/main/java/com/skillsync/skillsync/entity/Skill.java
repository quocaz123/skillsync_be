package com.skillsync.skillsync.entity;

import com.skillsync.skillsync.enums.SkillCategory;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Skill {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false, length = 100)
    String name;

    @Enumerated(EnumType.STRING)
    SkillCategory category;

    @Column(length = 50)
    String icon;

    @CreationTimestamp
    @Column(name = "created_at")
    LocalDateTime createdAt;
}
