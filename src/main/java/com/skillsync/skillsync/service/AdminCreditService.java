package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.AdminTransactionDTO;
import com.skillsync.skillsync.dto.GrantCreditRequest;
import com.skillsync.skillsync.entity.CreditTransaction;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.repository.CreditTransactionRepository;
import com.skillsync.skillsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCreditService {

    private final CreditTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public List<AdminTransactionDTO> getAllTransactions() {
        return transactionRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public AdminTransactionDTO grantCredit(GrantCreditRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        CreditTransaction transaction = CreditTransaction.builder()
                .user(user)
                .amount(request.getAmount())
                .transactionType(request.getTransactionType())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .build();

        // Update user balances
        user.setCreditsBalance(user.getCreditsBalance() + request.getAmount());
        userRepository.save(user);
        
        CreditTransaction savedTx = transactionRepository.save(transaction);
        return mapToDTO(savedTx);
    }

    private AdminTransactionDTO mapToDTO(CreditTransaction t) {
        boolean isSuspicious = Math.abs(t.getAmount()) > 500; // Over 500 is flagged
        
        return AdminTransactionDTO.builder()
                .id(t.getId().toString())
                .userId(t.getUser() != null ? t.getUser().getId().toString() : "")
                .userName(t.getUser() != null ? t.getUser().getFullName() : "Hệ thống")
                .userEmail(t.getUser() != null ? t.getUser().getEmail() : "")
                .userBalance(t.getUser() != null ? t.getUser().getCreditsBalance() : 0)
                .amount(t.getAmount())
                .transactionType(t.getTransactionType())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .suspicious(isSuspicious)
                .build();
    }
}
