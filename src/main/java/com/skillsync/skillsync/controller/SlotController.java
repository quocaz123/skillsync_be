package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.request.slot.CreateSlotsRequest;
import com.skillsync.skillsync.dto.response.slot.SlotResponse;
import com.skillsync.skillsync.service.SlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import com.skillsync.skillsync.dto.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teaching-skills")
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;

    /** GET /api/teaching-skills/{skillId}/slots — tất cả slot (teacher) */
    @GetMapping("/{teachingSkillId}/slots")
    public ApiResponse<List<SlotResponse>> getSlots(@PathVariable UUID teachingSkillId) {
        return ApiResponse.success(slotService.getSlotsByTeachingSkill(teachingSkillId));
    }

    /** GET /api/teaching-skills/{skillId}/slots/open — chỉ slot trống (public/learner) */
    @GetMapping("/{teachingSkillId}/slots/open")
    public ApiResponse<List<SlotResponse>> getOpenSlots(@PathVariable UUID teachingSkillId) {
        return ApiResponse.success(slotService.getOpenSlotsByTeachingSkill(teachingSkillId));
    }

    /**
     * POST /api/teaching-skills/{skillId}/slots/batch — tạo nhiều slot cùng lúc.
     * Body: { teachingSkillId, dates: [...], times: [...] }
     * Backend tạo tất cả tổ hợp (dates × times).
     */
    @PostMapping("/{teachingSkillId}/slots/batch")
    public ApiResponse<List<SlotResponse>> createSlots(
            @PathVariable UUID teachingSkillId,
            @RequestBody CreateSlotsRequest request) {
        request.setTeachingSkillId(teachingSkillId);
        return ApiResponse.success(slotService.createSlots(request));
    }

    /** DELETE /api/teaching-skills/{skillId}/slots/{slotId} — xóa slot (chưa booked) */
    @DeleteMapping("/{teachingSkillId}/slots/{slotId}")
    public ApiResponse<Void> deleteSlot(@PathVariable UUID teachingSkillId, @PathVariable UUID slotId) {
        slotService.deleteSlot(slotId);
        return ApiResponse.success(null);
    }
}
