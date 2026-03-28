package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.request.slot.CreateSlotsRequest;
import com.skillsync.skillsync.dto.response.slot.SlotResponse;
import com.skillsync.skillsync.service.SlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public List<SlotResponse> getSlots(@PathVariable UUID teachingSkillId) {
        return slotService.getSlotsByTeachingSkill(teachingSkillId);
    }

    /** GET /api/teaching-skills/{skillId}/slots/open — chỉ slot trống (public/learner) */
    @GetMapping("/{teachingSkillId}/slots/open")
    public List<SlotResponse> getOpenSlots(@PathVariable UUID teachingSkillId) {
        return slotService.getOpenSlotsByTeachingSkill(teachingSkillId);
    }

    /**
     * POST /api/teaching-skills/{skillId}/slots/batch — tạo nhiều slot cùng lúc.
     * Body: { teachingSkillId, dates: [...], times: [...] }
     * Backend tạo tất cả tổ hợp (dates × times).
     */
    @PostMapping("/{teachingSkillId}/slots/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<SlotResponse> createSlots(
            @PathVariable UUID teachingSkillId,
            @RequestBody CreateSlotsRequest request) {
        request.setTeachingSkillId(teachingSkillId);
        return slotService.createSlots(request);
    }

    /** DELETE /api/teaching-skills/{skillId}/slots/{slotId} — xóa slot (chưa booked) */
    @DeleteMapping("/{teachingSkillId}/slots/{slotId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSlot(@PathVariable UUID teachingSkillId, @PathVariable UUID slotId) {
        slotService.deleteSlot(slotId);
    }
}
