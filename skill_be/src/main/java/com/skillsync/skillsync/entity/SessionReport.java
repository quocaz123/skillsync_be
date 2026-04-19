package com.skillsync.skillsync.entity;

import com.skillsync.skillsync.enums.ReportReason;
import com.skillsync.skillsync.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "session_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SessionReport {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id")
    User reportedUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ReportReason reason;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "evidence_url", length = 500)
    String evidenceUrl;

    @Column(name = "counter_description", columnDefinition = "TEXT")
    String counterDescription;

    @Column(name = "counter_evidence_url", length = 500)
    String counterEvidenceUrl;

    @Column(name = "counter_submitted_at")
    LocalDateTime counterSubmittedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    ReportStatus status = ReportStatus.PENDING;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    String adminNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    User resolvedBy;

    @Column(name = "resolved_at")
    LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at")
    LocalDateTime createdAt;
}
