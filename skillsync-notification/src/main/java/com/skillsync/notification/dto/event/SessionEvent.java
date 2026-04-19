package com.skillsync.notification.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SessionEvent {

    /** Loại sự kiện (BOOKING_SUCCESS, BOOKING_CANCELLED, BOOKING_REMINDER) */
    String eventType;

    /** Email người nhận */
    String recipientEmail;

    /** Tên người nhận */
    String recipientName;

    /** Tên Mentor/Mentee (người đối diện) */
    String counterpartName;

    /** Thời gian phiên học định dạng String */
    String sessionTime;

    /** Link tham gia phiên (Google Meet / Zoom URL) */
    String sessionLink;

    /** Thời điểm sự kiện xảy ra */
    String timestamp;
}
