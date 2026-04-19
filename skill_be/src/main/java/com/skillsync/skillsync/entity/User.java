package com.skillsync.skillsync.entity;

import com.skillsync.skillsync.enums.Role;
import com.skillsync.skillsync.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false, unique = true)
    String email;

    @Column(nullable = false)
    String password;

    @Column(name = "full_name", nullable = false, columnDefinition = "varchar(255) default 'User'")
    String fullName = "User";

    @Column(name = "avatar_url", length = 500)
    String avatarUrl;

    @Column(name = "avatar_key", length = 255)
    String avatarKey;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    UserStatus status = UserStatus.ACTIVE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Role role = Role.USER;

    @Builder.Default
    @Column(name = "credits_balance")
    Integer creditsBalance = 50;


    @Builder.Default
    @Column(name = "violation_count")
    Integer violationCount = 0;

    @Builder.Default
    @Column(name = "is_email_verified", nullable = false)
    Boolean isEmailVerified = false;

    @Column(name = "otp_code", length = 6)
    String otpCode;

    @Column(name = "otp_expiry_time")
    LocalDateTime otpExpiryTime;

    @Column(columnDefinition = "TEXT")
    String bio;

    // True if user has explicitly set a password (false for new Google-only users)
    @Builder.Default
    @Column(name = "has_password", nullable = false)
    Boolean hasPassword = true;

    @CreationTimestamp
    @Column(name = "created_at")
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
