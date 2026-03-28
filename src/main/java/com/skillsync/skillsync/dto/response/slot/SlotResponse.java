package com.skillsync.skillsync.dto.response.slot;

import com.skillsync.skillsync.enums.SlotStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class SlotResponse {
    private UUID id;
    private UUID teachingSkillId;
    private String skillName;
    private LocalDate slotDate;
    private LocalTime slotTime;
    private SlotStatus status;
}
