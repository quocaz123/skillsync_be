package com.skillsync.skillsync.dto.response.admin;

import com.skillsync.skillsync.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminCreditTransactionResponse {
    private UUID id;
    private String userName;
    private String userEmail;
    private String userAvatarUrl;
    private Integer amount;
    private TransactionType transactionType;
    private String description;
    private UUID referenceId;
    private String userId;
    private Integer userBalance;
    private LocalDateTime createdAt;
}
