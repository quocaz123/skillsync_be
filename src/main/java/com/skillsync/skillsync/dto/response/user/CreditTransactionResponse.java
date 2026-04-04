package com.skillsync.skillsync.dto.response.user;

import com.skillsync.skillsync.enums.TransactionType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreditTransactionResponse {
    String id;
    Integer amount;
    TransactionType transactionType;
    String description;
    LocalDateTime createdAt;
}
