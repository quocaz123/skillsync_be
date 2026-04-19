package com.skillsync.skillsync.dto.request.skill;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateSkillPriceRequest {
    @NotNull(message = "newPrice không được để trống")
    @Min(value = 1, message = "Giá không được nhỏ hơn 1")
    @Max(value = 500, message = "Giá không được vượt quá giới hạn hệ thống (500 credits)")
    Integer newPrice;
}
