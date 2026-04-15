package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.CreditTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, UUID> {
    List<CreditTransaction> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
    boolean existsByReferenceIdAndTransactionType(UUID referenceId, com.skillsync.skillsync.enums.TransactionType type);
    List<CreditTransaction> findAllByOrderByCreatedAtDesc();
}
