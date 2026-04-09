package com.skillsync.skillsync.entity;

import com.skillsync.skillsync.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreditTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;

    @Column(nullable = false)
    Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    TransactionType transactionType;

    @Column(name = "reference_id")
    UUID referenceId;

    @Column(columnDefinition = "TEXT")
    String description;

    @CreationTimestamp
    @Column(name = "created_at")
    LocalDateTime createdAt;
}
