package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.request.session.BookSessionRequest;
import com.skillsync.skillsync.dto.response.session.SessionResponse;
import com.skillsync.skillsync.dto.response.session.ZegoTokenResponse;
import com.skillsync.skillsync.entity.Session;
import com.skillsync.skillsync.entity.TeachingSlot;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.enums.SessionStatus;
import com.skillsync.skillsync.enums.SlotStatus;
import com.skillsync.skillsync.exception.AppException;
import com.skillsync.skillsync.exception.ErrorCode;
import com.skillsync.skillsync.repository.SessionRepository;
import com.skillsync.skillsync.repository.TeachingSlotRepository;
import com.skillsync.skillsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final SessionRepository sessionRepository;
    private final TeachingSlotRepository slotRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ZegoTokenService zegoTokenService;

    // ── Book ────────────────────────────────────────────────
    @Transactional
    public SessionResponse book(BookSessionRequest request) {
        User learner = userService.getCurrentUser();

        TeachingSlot slot = slotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        if (slot.getStatus() != SlotStatus.OPEN) {
            throw new AppException(ErrorCode.SLOT_ALREADY_BOOKED);
        }
        if (slot.getTeacher().getId().equals(learner.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN); // teacher không tự book slot mình
        }

        // Trừ credits learner
        int cost = slot.getTeachingSkill().getCreditsPerHour();
        if (learner.getCreditsBalance() == null || learner.getCreditsBalance() < cost) {
            throw new AppException(ErrorCode.INSUFFICIENT_CREDITS);
        }
        learner.setCreditsBalance(learner.getCreditsBalance() - cost);
        userRepository.save(learner);

        // Đổi slot thành BOOKED
        slot.setStatus(SlotStatus.BOOKED);
        slotRepository.save(slot);

        // Tạo session với videoRoomId có prefix
        String videoRoomId = "skillsync_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Session session = Session.builder()
                .learner(learner)
                .teacher(slot.getTeacher())
                .slot(slot)
                .teachingSkill(slot.getTeachingSkill())
                .status(SessionStatus.SCHEDULED)
                .creditCost(cost)
                .videoRoomId(videoRoomId)
                .videoProvider("ZEGO")
                .learnerNotes(request.getLearnerNotes())
                .build();

        return toResponse(sessionRepository.save(session));
    }

    // ── Mine ────────────────────────────────────────────────
    public List<SessionResponse> getMySessions(String role, String status) {
        User user = userService.getCurrentUser();
        UUID userId = user.getId();
        SessionStatus statusEnum = parseStatus(status);
        boolean isLearner = "learner".equalsIgnoreCase(role);
        boolean isTeacher = "teacher".equalsIgnoreCase(role);

        List<Session> sessions;
        if (isTeacher && statusEnum != null) {
            sessions = sessionRepository.findByTeacherIdAndStatusOrderByCreatedAtDesc(userId, statusEnum);
        } else if (isTeacher) {
            sessions = sessionRepository.findByTeacherIdOrderByCreatedAtDesc(userId);
        } else if (isLearner && statusEnum != null) {
            sessions = sessionRepository.findByLearnerIdAndStatusOrderByCreatedAtDesc(userId, statusEnum);
        } else if (isLearner) {
            sessions = sessionRepository.findByLearnerIdOrderByCreatedAtDesc(userId);
        } else {
            // all: cả 2 chiều
            List<Session> learnerSessions = sessionRepository.findByLearnerIdOrderByCreatedAtDesc(userId);
            List<Session> teacherSessions = sessionRepository.findByTeacherIdOrderByCreatedAtDesc(userId);
            sessions = new java.util.ArrayList<>(learnerSessions);
            sessions.addAll(teacherSessions);
            sessions.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        }

        return sessions.stream().map(this::toResponse).toList();
    }

    // ── ZEGO Token (get token + optional mark join) ─────────
    public ZegoTokenResponse getZegoToken(UUID sessionId) {
        User user = userService.getCurrentUser();
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        // Chỉ teacher hoặc learner của session mới được lấy token
        boolean isParticipant = session.getTeacher().getId().equals(user.getId())
                || session.getLearner().getId().equals(user.getId());
        if (!isParticipant) throw new AppException(ErrorCode.FORBIDDEN);

        // Kiểm tra cửa sổ thời gian: -10 phút đến +2 tiếng so với giờ học
        LocalDateTime slotDateTime = LocalDateTime.of(
                session.getSlot().getSlotDate(),
                session.getSlot().getSlotTime());
        LocalDateTime now = LocalDateTime.now();
        // Cửa sổ join: cho phép từ 60 phút trước → 24 giờ sau giờ học
        // (Dành cho môi trường dev/test — production nên đặt lại -10min/+2h)
        if (now.isBefore(slotDateTime.minusMinutes(60))) {
            throw new AppException(ErrorCode.TOO_EARLY_TO_JOIN);
        }
        if (now.isAfter(slotDateTime.plusHours(24))) {
            throw new AppException(ErrorCode.SESSION_EXPIRED);
        }

        String token = zegoTokenService.generateToken(
                session.getVideoRoomId(),
                user.getId().toString(),
                user.getFullName() != null ? user.getFullName() : "User");

        return ZegoTokenResponse.builder()
                .token(token)
                .appId(zegoTokenService.getAppId())
                .roomId(session.getVideoRoomId())
                .userId(user.getId().toString())
                .userName(user.getFullName() != null ? user.getFullName() : "User")
                .build();
    }

    // ── Join (mark startedAt after ZEGO mounted) ────────────
    @Transactional
    public void markJoin(UUID sessionId) {
        User user = userService.getCurrentUser();
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        boolean isParticipant = session.getTeacher().getId().equals(user.getId())
                || session.getLearner().getId().equals(user.getId());
        if (!isParticipant) throw new AppException(ErrorCode.FORBIDDEN);

        // startedAt chỉ set khi người đầu tiên vào call (teacher hoặc learner đầu tiên join)
        if (session.getStartedAt() == null) {
            session.setStartedAt(LocalDateTime.now());
            sessionRepository.save(session);
        }
    }

    // ── Leave (mark endedAt) ────────────────────────────────
    @Transactional
    public void markLeave(UUID sessionId) {
        User user = userService.getCurrentUser();
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        boolean isParticipant = session.getTeacher().getId().equals(user.getId())
                || session.getLearner().getId().equals(user.getId());
        if (!isParticipant) throw new AppException(ErrorCode.FORBIDDEN);

        session.setEndedAt(LocalDateTime.now());
        session.setStatus(SessionStatus.COMPLETED);
        sessionRepository.save(session);
    }

    // ── Map ─────────────────────────────────────────────────
    private SessionResponse toResponse(Session s) {
        return SessionResponse.builder()
                .id(s.getId())
                .videoRoomId(s.getVideoRoomId())
                .videoProvider(s.getVideoProvider())
                .status(s.getStatus())
                .creditCost(s.getCreditCost())
                .slotDate(s.getSlot() != null ? s.getSlot().getSlotDate() : null)
                .slotTime(s.getSlot() != null ? s.getSlot().getSlotTime() : null)
                .teacherId(s.getTeacher() != null ? s.getTeacher().getId() : null)
                .teacherName(s.getTeacher() != null ? s.getTeacher().getFullName() : null)
                .teacherAvatar(s.getTeacher() != null ? s.getTeacher().getAvatarUrl() : null)
                .learnerId(s.getLearner() != null ? s.getLearner().getId() : null)
                .learnerName(s.getLearner() != null ? s.getLearner().getFullName() : null)
                .teachingSkillId(s.getTeachingSkill() != null ? s.getTeachingSkill().getId() : null)
                .skillName(s.getTeachingSkill() != null ? s.getTeachingSkill().getSkill().getName() : null)
                .skillIcon(s.getTeachingSkill() != null ? s.getTeachingSkill().getSkill().getIcon() : null)
                .learnerNotes(s.getLearnerNotes())
                .startedAt(s.getStartedAt())
                .endedAt(s.getEndedAt())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private SessionStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try { return SessionStatus.valueOf(status.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}
