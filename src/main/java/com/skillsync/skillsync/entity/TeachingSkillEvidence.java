package com.skillsync.skillsync.entity;

import com.skillsync.skillsync.enums.EvidenceType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "teaching_skill_evidences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeachingSkillEvidence {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teaching_skill_id", nullable = false)
    UserTeachingSkill teachingSkill;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false)
    EvidenceType evidenceType;

    @Column(nullable = false, length = 255)
    String title;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "file_name")
    String fileName;

    @Column(name = "file_url", length = 500)
    String fileUrl;

    @Column(name = "file_key", length = 255)
    String fileKey;

    @Column(name = "mime_type", length = 100)
    String mimeType;

    @Column(name = "file_size")
    Long fileSize;

    @Column(name = "external_url", length = 500)
    String externalUrl;

    @Builder.Default
    @Column(name = "is_verified", nullable = false)
    Boolean isVerified = false;

    @CreationTimestamp
    @Column(name = "created_at")
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
