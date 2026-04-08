package com.skillsync.skillsync.dto.response.user;

import com.skillsync.skillsync.enums.TransactionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreditTransactionResponse {
    UUID id;
    Integer amount;
    TransactionType transactionType;
    String description;
    LocalDateTime createdAt;
}
