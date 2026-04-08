package com.skillsync.skillsync.entity;

import com.skillsync.skillsync.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user", columnList = "user_id"),
        @Index(name = "idx_notification_user_read", columnList = "user_id,is_read"),
        @Index(name = "idx_notification_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    NotificationType type;

    @Column(nullable = false, length = 255)
    String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    String content;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    Boolean isRead = false;

    @Column(name = "redirect_url", length = 500)
    String redirectUrl;

    @Column(name = "entity_id")
    UUID entityId;

    @Column(name = "image_url", length = 500)
    String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}
