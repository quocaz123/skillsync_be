package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.request.slot.CreateSlotsRequest;
import com.skillsync.skillsync.dto.response.slot.SlotResponse;
import com.skillsync.skillsync.entity.TeachingSlot;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.entity.UserTeachingSkill;
import com.skillsync.skillsync.enums.SlotStatus;
import com.skillsync.skillsync.exception.AppException;
import com.skillsync.skillsync.exception.ErrorCode;
import com.skillsync.skillsync.repository.SessionRepository;
import com.skillsync.skillsync.repository.TeachingSlotRepository;
import com.skillsync.skillsync.repository.UserTeachingSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SlotService {

    private final TeachingSlotRepository slotRepository;
    private final UserTeachingSkillRepository teachingSkillRepository;
    private final SessionRepository sessionRepository;
    private final UserService userService;

    /** Teacher xem tất cả slot của một teaching skill */
    public List<SlotResponse> getSlotsByTeachingSkill(UUID teachingSkillId) {
        return slotRepository
                .findByTeachingSkillIdOrderBySlotDateAscSlotTimeAsc(teachingSkillId)
                .stream().map(this::toResponse).toList();
    }

    /** Teacher xem slot OPEN (public — cho Explore) */
    public List<SlotResponse> getOpenSlotsByTeachingSkill(UUID teachingSkillId) {
        return slotRepository
                .findByTeachingSkillIdAndStatusOrderBySlotDateAscSlotTimeAsc(teachingSkillId, SlotStatus.OPEN)
                .stream().map(this::toResponse).toList();
    }

    /**
     * Teacher tạo nhiều slot cùng lúc: cross-product của dates × times.
     * Bỏ qua các tổ hợp đã tồn tại.
     */
    @Transactional
    public List<SlotResponse> createSlots(CreateSlotsRequest request) {
        User teacher = userService.getCurrentUser();
        UserTeachingSkill skill = teachingSkillRepository.findById(request.getTeachingSkillId())
                .orElseThrow(() -> new AppException(ErrorCode.SKILL_NOT_FOUND));

        // Chỉ owner mới tạo được
        if (!skill.getUser().getId().equals(teacher.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        List<TeachingSlot> toSave = new ArrayList<>();
        for (CreateSlotsRequest.SlotEntry entry : request.getSlots()) {
            // Bỏ qua nếu đã tồn tại slot cùng ngày + giờ
            if (!slotRepository.existsByTeachingSkillIdAndSlotDateAndSlotTime(
                    skill.getId(), entry.getDate(), entry.getTime())) {
                toSave.add(TeachingSlot.builder()
                        .teacher(teacher)
                        .teachingSkill(skill)
                        .slotDate(entry.getDate())
                        .slotTime(entry.getTime())
                        .slotEndTime(entry.getEndTime())
                        .creditCost(entry.getCreditCost())
                        .status(SlotStatus.OPEN)
                        .build());
            }
        }

        return slotRepository.saveAll(toSave).stream().map(this::toResponse).toList();
    }

    /**
     * Teacher xóa slot — chỉ khi slot chưa bị BOOKED / có session.
     */
    @Transactional
    public void deleteSlot(UUID slotId) {
        TeachingSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        User teacher = userService.getCurrentUser();
        if (slot.getTeacher().getId().equals(teacher.getId()) == false) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        if (slot.getStatus() == SlotStatus.BOOKED || sessionRepository.existsBySlotIdAndStatusNot(slotId, com.skillsync.skillsync.enums.SessionStatus.CANCELLED)) {
            throw new AppException(ErrorCode.SLOT_ALREADY_BOOKED);
        }
        slotRepository.delete(slot);
    }

    private SlotResponse toResponse(TeachingSlot s) {
        return SlotResponse.builder()
                .id(s.getId())
                .teachingSkillId(s.getTeachingSkill().getId())
                .skillName(s.getTeachingSkill().getSkill().getName())
                .slotDate(s.getSlotDate())
                .slotTime(s.getSlotTime())
                .slotEndTime(s.getSlotEndTime())
                .creditCost(s.getCreditCost())
                .status(s.getStatus())
                .build();
    }
}
