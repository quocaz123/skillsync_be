package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.TeachingSlot;
import com.skillsync.skillsync.enums.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TeachingSlotRepository extends JpaRepository<TeachingSlot, UUID> {
    interface TeachingSkillStats {
        UUID getTeachingSkillId();
        Long getOpenSlots();
        Long getTotalSessions();
    }

    @Query("SELECT s.teachingSkill.id AS teachingSkillId, " +
           "SUM(CASE WHEN s.status = 'OPEN' THEN 1 ELSE 0 END) AS openSlots, " +
           "SUM(CASE WHEN s.status = 'BOOKED' THEN 1 ELSE 0 END) AS totalSessions " +
           "FROM TeachingSlot s " +
           "WHERE s.teachingSkill.id IN :skillIds " +
           "GROUP BY s.teachingSkill.id")
    List<TeachingSkillStats> getStatsBySkillIds(@Param("skillIds") List<UUID> skillIds);

    List<TeachingSlot> findByTeachingSkillIdOrderBySlotDateAscSlotTimeAsc(UUID teachingSkillId);
    List<TeachingSlot> findByTeachingSkillIdAndStatusOrderBySlotDateAscSlotTimeAsc(UUID teachingSkillId, SlotStatus status);
    boolean existsByTeachingSkillIdAndSlotDateAndSlotTime(UUID teachingSkillId, java.time.LocalDate date, java.time.LocalTime time);

    List<TeachingSlot> findByTeachingSkillIdAndSlotDateInAndStatusNot(UUID teachingSkillId, List<LocalDate> slotDates, SlotStatus status);

    /** Lấy các slot theo status và ngày <= hôm nay (để auto-expire slot quá hạn). */
    List<TeachingSlot> findByStatusAndSlotDateLessThanEqual(SlotStatus status, LocalDate date);
}
