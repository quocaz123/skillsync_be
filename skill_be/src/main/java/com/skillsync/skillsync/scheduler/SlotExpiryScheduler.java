package com.skillsync.skillsync.scheduler;

import com.skillsync.skillsync.entity.TeachingSlot;
import com.skillsync.skillsync.enums.SlotStatus;
import com.skillsync.skillsync.repository.TeachingSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Tự động "đóng" slot OPEN đã quá thời gian học (không có ai book/approve).
 *
 * <p>Hiện project chưa có trạng thái EXPIRED nên dùng CANCELLED để slot không còn hiển thị ở Explore.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SlotExpiryScheduler {

    private final TeachingSlotRepository slotRepository;

    @Value("${app.timezone:Asia/Ho_Chi_Minh}")
    private String appTimezone;

    /**
     * Chạy mỗi 5 phút.
     * - Chỉ xử lý slot status=OPEN và slotDate <= hôm nay
     * - Nếu endDateTime < now ⇒ set CANCELLED
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void expireOpenSlots() {
        ZoneId zone = ZoneId.of(appTimezone);
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDate today = now.toLocalDate();

        List<TeachingSlot> candidates = slotRepository.findByStatusAndSlotDateLessThanEqual(SlotStatus.OPEN, today);
        if (candidates.isEmpty()) return;

        int changed = 0;
        for (TeachingSlot slot : candidates) {
            if (slot.getSlotDate() == null || slot.getSlotTime() == null) continue;

            var start = slot.getSlotDate().atTime(slot.getSlotTime());
            var end = slot.getSlotEndTime() != null ? slot.getSlotDate().atTime(slot.getSlotEndTime()) : start.plusHours(1);
            if (!end.isAfter(start)) {
                end = start.plusHours(1);
            }

            if (end.isBefore(now)) {
                slot.setStatus(SlotStatus.CANCELLED);
                changed++;
            }
        }

        if (changed > 0) {
            slotRepository.saveAll(candidates);
            log.info("[SlotExpiry] Cancelled {} expired OPEN slot(s).", changed);
        }
    }
}

