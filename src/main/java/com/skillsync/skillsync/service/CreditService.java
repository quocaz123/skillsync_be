package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.response.user.CreditTransactionResponse;
import com.skillsync.skillsync.entity.CreditTransaction;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.repository.CreditTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditTransactionRepository creditTransactionRepository;
    private final UserService userService;

    public List<CreditTransactionResponse> getMyCreditHistory() {
        User user = userService.getCurrentUser();
        List<CreditTransaction> transactions = creditTransactionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
        
        return transactions.stream().map(tx -> CreditTransactionResponse.builder()
                .id(tx.getId())
                .amount(tx.getAmount())
                .transactionType(tx.getTransactionType())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build()).collect(Collectors.toList());
    }
}
