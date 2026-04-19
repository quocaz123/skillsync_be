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
    @Column(name = "trust_score")
    Integer trustScore = 50;

    @Column(columnDefinition = "TEXT")
    String bio;

    // True if user has explicitly set a password (false for new Google-only users)
    @Builder.Default
    @Column(name = "has_password", nullable = false)
    Boolean hasPassword = true;

    @Builder.Default
    @Column(name = "email_verified", nullable = false, columnDefinition = "boolean default false")
    Boolean emailVerified = false;

    @Column(name = "email_verification_code_hash", length = 255)
    String emailVerificationCodeHash;

    @Column(name = "email_verification_expires_at")
    LocalDateTime emailVerificationExpiresAt;

    @Column(name = "password_reset_code_hash", length = 255)
    String passwordResetCodeHash;

    @Column(name = "password_reset_expires_at")
    LocalDateTime passwordResetExpiresAt;

    @CreationTimestamp
    @Column(name = "created_at")
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    /** Thời điểm đăng nhập thành công lần đầu (null = chưa từng đăng nhập). Đăng ký không tính là đăng nhập. */
    @Column(name = "first_login_at")
    LocalDateTime firstLoginAt;
}
