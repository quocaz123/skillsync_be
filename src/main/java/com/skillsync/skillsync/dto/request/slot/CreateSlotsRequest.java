package com.skillsync.skillsync.dto.request.slot;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
public class CreateSlotsRequest {
    private UUID teachingSkillId;
    // Batch: nhiều ngày + nhiều giờ → tạo tất cả tổ hợp
    private List<LocalDate> dates;
    private List<LocalTime> times;
}
