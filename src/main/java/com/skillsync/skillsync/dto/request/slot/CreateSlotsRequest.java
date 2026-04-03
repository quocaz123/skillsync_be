package com.skillsync.skillsync.dto.request.slot;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
public class CreateSlotsRequest {
    private UUID teachingSkillId;

    /**
     * Danh sách slot từng cái — mỗi slot có ngày, giờ bắt đầu, giờ kết thúc, và số credits riêng.
     */
    private List<SlotEntry> slots;

    @Data
    public static class SlotEntry {
        private LocalDate date;
        private LocalTime time;
        private LocalTime endTime;    // optional
        private Integer creditCost;  // số credits học viên cần trả cho slot này
    }
}
