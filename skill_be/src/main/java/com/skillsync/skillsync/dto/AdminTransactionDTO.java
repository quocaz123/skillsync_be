package com.skillsync.skillsync.dto;

import com.skillsync.skillsync.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminTransactionDTO {
    private String id;
    private String userId;
    private String userName;
    private String userEmail;
    private Integer userBalance;
    private Integer amount;
    private TransactionType transactionType;
    private String description;
    private LocalDateTime createdAt;
    private boolean suspicious;
}
