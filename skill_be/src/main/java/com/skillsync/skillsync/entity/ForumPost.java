package com.skillsync.skillsync.entity;

import com.skillsync.skillsync.enums.PostType;
import com.skillsync.skillsync.enums.ForumPostStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "forum_posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ForumPost {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    ForumCategory category;

    @Column(nullable = false)
    String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    String content;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "post_type")
    PostType postType = PostType.DISCUSSION;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    ForumPostStatus status = ForumPostStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    User reviewedBy;

    @Column(name = "reviewed_at")
    LocalDateTime reviewedAt;

    @Builder.Default
    @Column(name = "solved")
    Boolean solved = false;

    String tags;

    @CreationTimestamp
    @Column(name = "created_at")
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
