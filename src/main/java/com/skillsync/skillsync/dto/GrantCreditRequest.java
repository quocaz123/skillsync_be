package com.skillsync.skillsync.dto;

import com.skillsync.skillsync.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class GrantCreditRequest {
    @NotNull(message = "User ID không được để trống")
    private UUID userId;

    @NotNull(message = "Số lượng không được để trống")
    private Integer amount;

    @NotNull(message = "Loại giao dịch không được để trống")
    private TransactionType transactionType;

    private String description;
}
