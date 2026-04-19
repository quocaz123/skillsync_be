package com.skillsync.skillsync.dto.request.session;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProposeSessionRequest {
    @NotNull(message = "teachingSkillId không được để trống")
    UUID teachingSkillId;

    @NotNull(message = "slotDate không được để trống")
    LocalDate slotDate;

    @NotNull(message = "slotTime không được để trống")
    LocalTime slotTime;
    
    LocalTime slotEndTime;

    String learnerNotes;
}
