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
                .amount(normalizeAmountByType(tx.getAmount(), tx.getTransactionType()))
                .transactionType(tx.getTransactionType())
                .description(toVietnameseDescription(tx))
                .createdAt(tx.getCreatedAt())
                .build()).collect(Collectors.toList());
    }

    private Integer normalizeAmountByType(Integer rawAmount, com.skillsync.skillsync.enums.TransactionType type) {
        int value = rawAmount != null ? rawAmount : 0;
        int absValue = Math.abs(value);
        return switch (type) {
            case SPEND_SESSION, SPEND_LEARNING_PATH, PENALTY -> -absValue;
            case EARN_SESSION, EARN_LEARNING_PATH, MISSION_REWARD, REFUND, WELCOME_BONUS -> absValue;
        };
    }

    private String toVietnameseDescription(CreditTransaction tx) {
        String fallback = tx.getDescription() != null ? tx.getDescription() : "Giao dịch credits";
        if (tx.getTransactionType() == null) return fallback;

        return switch (tx.getTransactionType()) {
            case SPEND_SESSION -> "Thanh toán buổi học";
            case EARN_SESSION -> "Nhận credits từ buổi dạy";
            case SPEND_LEARNING_PATH -> "Thanh toán lộ trình học";
            case EARN_LEARNING_PATH -> "Nhận credits từ lộ trình học";
            case MISSION_REWARD -> "Thưởng nhiệm vụ";
            case REFUND -> "Hoàn credits";
            case WELCOME_BONUS -> "Thưởng chào mừng";
            case PENALTY -> "Khấu trừ credits";
        };
    }
}
