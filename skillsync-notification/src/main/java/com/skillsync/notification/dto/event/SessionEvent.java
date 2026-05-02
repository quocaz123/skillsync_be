package com.skillsync.notification.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SessionEvent {

    /**
     * Loại sự kiện.
     * <p>
     * Legacy (template-driven): BOOKING_SUCCESS, BOOKING_CANCELLED, BOOKING_REMINDER
     * <br>
     * Domain (từ skill_be): SESSION_BOOKED, SESSION_APPROVED, SESSION_REJECTED, SESSION_CANCELLED
     */
    String eventType;

    /** Email người nhận */
    String recipientEmail;

    /** Tên người nhận */
    String recipientName;

    /** Tên Mentor/Mentee (người đối diện) — dùng bởi legacy templates */
    String counterpartName;

    /** Thời gian phiên học định dạng String — dùng bởi legacy templates */
    String sessionTime;

    /** Link tham gia phiên (Google Meet / Zoom URL) — dùng bởi legacy templates */
    String sessionLink;

    // ── Domain fields từ skill_be (optional) ──────────────────────────────

    /** Tên người gửi (mentor hoặc learner) */
    String senderName;

    String skillName;
    String slotDate;
    String slotTime;
    Integer creditCost;
    String sessionId;

    /** Thời điểm sự kiện xảy ra */
    String timestamp;
}
