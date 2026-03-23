package com.skillsync.skillsync.entity;

import com.skillsync.skillsync.enums.EvidenceType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_evidences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserEvidence {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;

    @Enumerated(EnumType.STRING)
    EvidenceType type;

    String label;

    @Column(name = "evidence_url", length = 500)
    String evidenceUrl;

    @Builder.Default
    @Column(name = "is_verified")
    Boolean isVerified = false;

    @CreationTimestamp
    @Column(name = "created_at")
    LocalDateTime createdAt;
}
