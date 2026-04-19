package com.skillsync.notification.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreditEvent {

    /** Loại sự kiện (DEPOSIT_SUCCESS, WITHDRAWAL_SUCCESS) */
    String eventType;

    /** Email người nhận */
    String recipientEmail;

    /** Tên người nhận */
    String recipientName;

    /** Số tiền giao dịch */
    String amount;

    /** Số dư còn lại */
    String balance;

    /** Thời điểm sự kiện xảy ra */
    String timestamp;
}
