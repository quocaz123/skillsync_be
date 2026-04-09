package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.TeachingSlot;
import com.skillsync.skillsync.enums.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TeachingSlotRepository extends JpaRepository<TeachingSlot, UUID> {
    List<TeachingSlot> findByTeachingSkillIdOrderBySlotDateAscSlotTimeAsc(UUID teachingSkillId);
    List<TeachingSlot> findByTeachingSkillIdAndStatusOrderBySlotDateAscSlotTimeAsc(UUID teachingSkillId, SlotStatus status);
    boolean existsByTeachingSkillIdAndSlotDateAndSlotTime(UUID teachingSkillId, java.time.LocalDate date, java.time.LocalTime time);

    List<TeachingSlot> findByTeachingSkillIdAndSlotDateInAndStatusNot(UUID teachingSkillId, List<LocalDate> slotDates, SlotStatus status);
}
