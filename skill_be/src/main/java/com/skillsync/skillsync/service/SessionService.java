package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.request.session.BookSessionRequest;
import com.skillsync.skillsync.dto.request.session.ProposeSessionRequest;
import com.skillsync.skillsync.dto.response.session.SessionResponse;
import com.skillsync.skillsync.dto.response.session.ZegoTokenResponse;
import com.skillsync.skillsync.entity.Session;
import com.skillsync.skillsync.entity.TeachingSlot;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.entity.UserTeachingSkill;
import com.skillsync.skillsync.enums.SessionStatus;
import com.skillsync.skillsync.enums.SlotStatus;
import com.skillsync.skillsync.exception.AppException;
import com.skillsync.skillsync.exception.ErrorCode;
import com.skillsync.skillsync.entity.CreditTransaction;
import com.skillsync.skillsync.enums.TransactionType;
import com.skillsync.skillsync.repository.CreditTransactionRepository;
import com.skillsync.skillsync.repository.SessionRepository;
import com.skillsync.skillsync.repository.TeachingSlotRepository;
import com.skillsync.skillsync.repository.UserRepository;
import com.skillsync.skillsync.repository.UserTeachingSkillRepository;
import com.skillsync.skillsync.repository.SessionReportRepository;
import com.skillsync.skillsync.dto.request.notification.NotificationCreateRequest;
import com.skillsync.skillsync.enums.NotificationType;
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
    private final CreditTransactionRepository transactionRepository;
    private final NotificationService notificationService;
    private final SystemLogService systemLogService;
    private final UserTeachingSkillRepository userTeachingSkillRepository;
    private final NotificationEventPublisher notificationEventPublisher;
    private final SessionReportRepository sessionReportRepository;

    // ── Book (Request) ──────────────────────────────────────
    @Transactional
    public SessionResponse book(BookSessionRequest request) {
        User learner = userService.getCurrentUser();

        TeachingSlot slot = slotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw new AppException(ErrorCode.SLOT_ALREADY_BOOKED);
        }
        if (slot.getTeacher().getId().equals(learner.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN); // teacher không tự book slot mình
        }

        if (slot.getTeachingSkill() != null && slot.getTeachingSkill().isHidden()) {
            throw new AppException(ErrorCode.TEACHING_SKILL_NOT_ACCEPTING);
        }

        // Không cho học viên đặt slot bị trùng/overlap với lịch học hiện có của chính họ
        java.time.LocalTime slotStart = slot.getSlotTime();
        java.time.LocalTime slotEnd = normalizeEndTime(slot.getSlotTime(), slot.getSlotEndTime());
        if (slotStart == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (slotEnd != null && !slotEnd.isAfter(slotStart)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        List<Session> learnerSessions = sessionRepository.findByLearnerIdAndStatusNotOrderByCreatedAtDesc(
                learner.getId(), SessionStatus.CANCELLED);
        for (Session s : learnerSessions) {
            if (s.getSlot() == null) continue;
            if (s.getSlot().getSlotDate() == null || s.getSlot().getSlotTime() == null) continue;
            if (!s.getSlot().getSlotDate().equals(slot.getSlotDate())) continue;

            java.time.LocalTime otherStart = s.getSlot().getSlotTime();
            java.time.LocalTime otherEnd = normalizeEndTime(otherStart, s.getSlot().getSlotEndTime());
            if (isOverlap(otherStart, otherEnd, slotStart, slotEnd)) {
                throw new AppException(ErrorCode.SESSION_TIME_CONFLICT);
            }
        }

        int cost = slot.getCreditCost() != null ? slot.getCreditCost() : slot.getTeachingSkill().getCreditsPerHour();
        if (learner.getCreditsBalance() == null || learner.getCreditsBalance() < cost) {
            throw new AppException(ErrorCode.INSUFFICIENT_CREDITS);
        }

        // Tạo session với trạng thái PENDING_APPROVAL, chưa trừ credits
        String videoRoomId = "skillsync_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Session session = Session.builder()
                .learner(learner)
                .teacher(slot.getTeacher())
                .slot(slot)
                .teachingSkill(slot.getTeachingSkill())
                .status(SessionStatus.PENDING_APPROVAL)
                .creditCost(cost)
                .videoRoomId(videoRoomId)
                .videoProvider("ZEGO")
                .learnerNotes(request.getLearnerNotes())
                .build();

        session = sessionRepository.save(session);

        // Thông báo cho Mentor
        notificationService.createAndSend(NotificationCreateRequest.builder()
                .userId(slot.getTeacher().getId())
                .type(NotificationType.SESSION_BOOKED)
                .title("Yêu cầu đặt lịch mới")
                .content(learner.getFullName() + " muốn đặt lịch học kỹ năng " + slot.getTeachingSkill().getSkill().getName() + ".")
                .entityId(session.getId())
                .redirectUrl("/app/teaching")
                .build());

        // Publish qua Kafka để gửi Email
        notificationEventPublisher.publishSessionEvent(
                "SESSION_BOOKED",
                slot.getTeacher().getEmail(),
                slot.getTeacher().getFullName(),
                learner.getFullName(),
                slot.getTeachingSkill().getSkill().getName(),
                slot.getSlotDate() != null ? slot.getSlotDate().toString() : "",
                slot.getSlotTime() != null ? slot.getSlotTime().toString() : "",
                cost,
                session.getId().toString()
        );

        return toResponse(session);
    }

    @Transactional
    public SessionResponse proposeSession(ProposeSessionRequest request) {
        User learner = userService.getCurrentUser();

        UserTeachingSkill teachingSkill = userTeachingSkillRepository.findById(request.getTeachingSkillId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        if (teachingSkill.getVerificationStatus() != com.skillsync.skillsync.enums.VerificationStatus.APPROVED) {
            throw new AppException(ErrorCode.FORBIDDEN); // Chỉ skill APPROVED mới được propose
        }

        if (teachingSkill.isHidden()) {
            throw new AppException(ErrorCode.TEACHING_SKILL_NOT_ACCEPTING);
        }

        if (teachingSkill.getUser().getId().equals(learner.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN); // Teacher không được tự book chính mình
        }

        // Validate time
        if (request.getSlotEndTime() != null && !request.getSlotEndTime().isAfter(request.getSlotTime())) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        
        java.time.LocalTime slotStart = request.getSlotTime();
        java.time.LocalTime slotEnd = normalizeEndTime(slotStart, request.getSlotEndTime());

        // Validate conflicts with learner's own schedule
        List<Session> learnerSessions = sessionRepository.findByLearnerIdAndStatusNotOrderByCreatedAtDesc(
                learner.getId(), SessionStatus.CANCELLED);
        for (Session s : learnerSessions) {
            if (s.getSlot() == null) continue;
            if (s.getSlot().getSlotDate() == null || s.getSlot().getSlotTime() == null) continue;
            if (!s.getSlot().getSlotDate().equals(request.getSlotDate())) continue;

            java.time.LocalTime otherStart = s.getSlot().getSlotTime();
            java.time.LocalTime otherEnd = normalizeEndTime(otherStart, s.getSlot().getSlotEndTime());
            if (isOverlap(otherStart, otherEnd, slotStart, slotEnd)) {
                throw new AppException(ErrorCode.SESSION_TIME_CONFLICT);
            }
        }

        // Calculate cost based on duration
        long durationHours = java.time.Duration.between(slotStart, slotEnd).toHours();
        if (durationHours == 0) durationHours = 1; // Default minimum 1 hour
        int cost = (int) durationHours * teachingSkill.getCreditsPerHour();

        if (learner.getCreditsBalance() == null || learner.getCreditsBalance() < cost) {
            throw new AppException(ErrorCode.INSUFFICIENT_CREDITS);
        }

        // Create a dedicated BOOKED slot for this proposal
        TeachingSlot generatedSlot = TeachingSlot.builder()
                .teacher(teachingSkill.getUser())
                .teachingSkill(teachingSkill)
                .slotDate(request.getSlotDate())
                .slotTime(slotStart)
                .slotEndTime(slotEnd)
                .creditCost(cost)
                .status(SlotStatus.PENDING) // Đổi sang PENDING thay vì BOOKED ngay lập tức
                .build();
        generatedSlot = slotRepository.save(generatedSlot);

        // Create Session
        String videoRoomId = "skillsync_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Session session = Session.builder()
                .learner(learner)
                .teacher(teachingSkill.getUser())
                .slot(generatedSlot)
                .teachingSkill(teachingSkill)
                .status(SessionStatus.PENDING_APPROVAL)
                .creditCost(cost)
                .videoRoomId(videoRoomId)
                .videoProvider("ZEGO")
                .learnerNotes(request.getLearnerNotes())
                .build();

        session = sessionRepository.save(session);

        // Notify Mentor
        notificationService.createAndSend(NotificationCreateRequest.builder()
                .userId(teachingSkill.getUser().getId())
                .type(NotificationType.SESSION_BOOKED)
                .title("Có đề xuất mở lớp ngoài giờ")
                .content(learner.getFullName() + " muốn học kỹ năng " + teachingSkill.getSkill().getName() + " vào một khung giờ tự đề xuất.")
                .entityId(session.getId())
                .redirectUrl("/app/teaching")
                .build());

        // Publish qua Kafka để gửi Email
        notificationEventPublisher.publishSessionEvent(
                "SESSION_BOOKED",
                teachingSkill.getUser().getEmail(),
                teachingSkill.getUser().getFullName(),
                learner.getFullName(),
                teachingSkill.getSkill().getName(),
                request.getSlotDate() != null ? request.getSlotDate().toString() : "",
                slotStart != null ? slotStart.toString() : "",
                cost,
                session.getId().toString()
        );

        return toResponse(session);
    }

    private static java.time.LocalTime normalizeEndTime(java.time.LocalTime start, java.time.LocalTime end) {
        if (start == null) return end;
        if (end == null) return start.plusHours(1);
        return end;
    }

    private static boolean isOverlap(java.time.LocalTime aStart, java.time.LocalTime aEnd,
                                     java.time.LocalTime bStart, java.time.LocalTime bEnd) {
        if (aStart == null || aEnd == null || bStart == null || bEnd == null) return false;
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    // ── Approve/Reject ──────────────────────────────────────
    @Transactional
    public SessionResponse approveSession(UUID sessionId) {
        User teacher = userService.getCurrentUser();
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        if (!session.getTeacher().getId().equals(teacher.getId())) {
            throw new AppException(ErrorCode.NOT_SESSION_TEACHER);
        }
        if (session.getStatus() != SessionStatus.PENDING_APPROVAL) {
            throw new AppException(ErrorCode.SESSION_ALREADY_DECIDED);
        }

        User learner = session.getLearner();
        int cost = session.getCreditCost();

        if (learner.getCreditsBalance() == null || learner.getCreditsBalance() < cost) {
            throw new AppException(ErrorCode.INSUFFICIENT_CREDITS);
        }

        // Trừ credits
        learner.setCreditsBalance(learner.getCreditsBalance() - cost);
        userRepository.save(learner);

        // Lưu transaction
        CreditTransaction tx = CreditTransaction.builder()
                .user(learner)
                .amount(cost)
                .transactionType(TransactionType.SPEND_SESSION)
                .referenceId(session.getId())
                .description("Paid for session " + session.getVideoRoomId())
                .build();
        transactionRepository.save(tx);

        // Chuyển session -> SCHEDULED
        session.setStatus(SessionStatus.SCHEDULED);
        sessionRepository.save(session);

        // Chuyển slot -> BOOKED
        TeachingSlot slot = session.getSlot();
        slot.setStatus(SlotStatus.BOOKED);
        slotRepository.save(slot);

        // Huỷ các PENDING khác
        List<Session> otherPending = sessionRepository.findBySlotIdAndStatus(slot.getId(), SessionStatus.PENDING_APPROVAL);
        for (Session pendingSession : otherPending) {
            if (!pendingSession.getId().equals(session.getId())) {
                pendingSession.setStatus(SessionStatus.CANCELLED);
                sessionRepository.save(pendingSession);

                notificationService.createAndSend(NotificationCreateRequest.builder()
                        .userId(pendingSession.getLearner().getId())
                        .type(NotificationType.SESSION_CANCELLED)
                        .title("Lịch học không khả dụng")
                        .content("Mentor đã nhận học viên khác cho slot này.")
                        .entityId(pendingSession.getId())
                        .redirectUrl("/app/sessions")
                        .build());

                // Publish qua Kafka để gửi Email báo bị nhường slot
                notificationEventPublisher.publishSessionEvent(
                        "SESSION_CANCELLED",
                        pendingSession.getLearner().getEmail(),
                        pendingSession.getLearner().getFullName(),
                        teacher.getFullName(),
                        pendingSession.getTeachingSkill().getSkill().getName(),
                        pendingSession.getSlot() != null && pendingSession.getSlot().getSlotDate() != null ? pendingSession.getSlot().getSlotDate().toString() : "",
                        pendingSession.getSlot() != null && pendingSession.getSlot().getSlotTime() != null ? pendingSession.getSlot().getSlotTime().toString() : "",
                        pendingSession.getCreditCost(),
                        pendingSession.getId().toString()
                );
            }
        }

        // Thông báo Learner được duyệt
        notificationService.createAndSend(NotificationCreateRequest.builder()
                .userId(learner.getId())
                .type(NotificationType.SESSION_APPROVED)
                .title("Lịch học được chấp nhận")
                .content(teacher.getFullName() + " đã đồng ý dạy. Credits đã bị trừ.")
                .entityId(session.getId())
                .redirectUrl("/app/sessions")
                .build());

        // Publish Kafka để gửi Email báo đã nhận lịch
        notificationEventPublisher.publishSessionEvent(
                "SESSION_APPROVED",
                learner.getEmail(),
                learner.getFullName(),
                teacher.getFullName(),
                session.getTeachingSkill().getSkill().getName(),
                session.getSlot() != null && session.getSlot().getSlotDate() != null ? session.getSlot().getSlotDate().toString() : "",
                session.getSlot() != null && session.getSlot().getSlotTime() != null ? session.getSlot().getSlotTime().toString() : "",
                cost,
                session.getId().toString()
        );

        return toResponse(session);
    }

    @Transactional
    public void rejectSession(UUID sessionId) {
        User teacher = userService.getCurrentUser();
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        if (!session.getTeacher().getId().equals(teacher.getId())) {
            throw new AppException(ErrorCode.NOT_SESSION_TEACHER);
        }
        if (session.getStatus() != SessionStatus.PENDING_APPROVAL) {
            throw new AppException(ErrorCode.SESSION_ALREADY_DECIDED);
        }

        session.setStatus(SessionStatus.CANCELLED);
        sessionRepository.save(session);

        // Nếu slot này là PENDING (do đề xuất), chuyển sang CANCELLED
        TeachingSlot slot = session.getSlot();
        if (slot != null && slot.getStatus() == SlotStatus.PENDING) {
            slot.setStatus(SlotStatus.CANCELLED);
            slotRepository.save(slot);
        }

        // Thông báo Learner bị từ chối
        notificationService.createAndSend(NotificationCreateRequest.builder()
                .userId(session.getLearner().getId())
                .type(NotificationType.SESSION_REJECTED)
                .title("Lịch học bị từ chối")
                .content(teacher.getFullName() + " đã từ chối yêu cầu của bạn.")
                .entityId(session.getId())
                .redirectUrl("/app/sessions")
                .build());

        // Publish Kafka để gửi Email báo bị huỷ
        notificationEventPublisher.publishSessionEvent(
                "SESSION_REJECTED",
                session.getLearner().getEmail(),
                session.getLearner().getFullName(),
                teacher.getFullName(),
                session.getTeachingSkill().getSkill().getName(),
                session.getSlot() != null && session.getSlot().getSlotDate() != null ? session.getSlot().getSlotDate().toString() : "",
                session.getSlot() != null && session.getSlot().getSlotTime() != null ? session.getSlot().getSlotTime().toString() : "",
                session.getCreditCost(),
                session.getId().toString()
        );
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

    // ── Admin: All Sessions ─────────────────────────────────
    public List<SessionResponse> getAllSessionsForAdmin(SessionStatus status) {
        List<Session> sessions;
        if (status != null) {
            sessions = sessionRepository.findByStatusInOrderByCreatedAtDesc(List.of(status));
        } else {
            sessions = sessionRepository.findAll().stream()
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    })
                    .toList();
        }
        return sessions.stream().map(this::toResponse).toList();
    }

    // ── Admin Escrow Management ─────────────────────────────
    public List<SessionResponse> getEscrowSessions() {
        // Lấy tất cả các session đang giữ tiền (KHÔNG PHẢI CANCELLED VÀ CHƯA CÓ
        // EARN_SESSION / REFUND)
        // MVP: Lấy các session đang ở trạng thái SCHEDULED, IN_PROGRESS, COMPLETED,
        // DISPUTED
        List<Session> escrowSessions = sessionRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(SessionStatus.SCHEDULED, SessionStatus.IN_PROGRESS, SessionStatus.COMPLETED,
                        SessionStatus.DISPUTED));
        return escrowSessions.stream().map(this::toResponse).toList();
    }

    // ── ZEGO Token (get token + optional mark join) ─────────
    public ZegoTokenResponse getZegoToken(UUID sessionId) {
        User user = userService.getCurrentUser();
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        // Chỉ teacher hoặc learner của session mới được lấy token
        boolean isParticipant = session.getTeacher().getId().equals(user.getId())
                || session.getLearner().getId().equals(user.getId());
        if (!isParticipant)
            throw new AppException(ErrorCode.FORBIDDEN);

        // Kiểm tra cửa sổ thời gian: -24 giờ đến +24 giờ so với giờ học (DEV MODE)
        LocalDateTime slotDateTime = LocalDateTime.of(
                session.getSlot().getSlotDate(),
                session.getSlot().getSlotTime());
        LocalDateTime now = LocalDateTime.now();
        // Cửa sổ join (DEV MODE): cho phép từ 24 giờ trước → 24 giờ sau giờ học
        if (now.isBefore(slotDateTime.minusHours(24))) {
            throw new AppException(ErrorCode.TOO_EARLY_TO_JOIN);
        }
        if (now.isAfter(slotDateTime.plusHours(24))) {
            throw new AppException(ErrorCode.SESSION_EXPIRED);
        }

        // Tính toán thời gian hết hạn của token dựa trên thời gian kết thúc của slot
        LocalDateTime slotEndDateTime;
        if (session.getSlot().getSlotEndTime() != null) {
            slotEndDateTime = LocalDateTime.of(
                    session.getSlot().getSlotDate(),
                    session.getSlot().getSlotEndTime());
        } else {
            // Fallback nếu không có slotEndTime, mặc định là 1 tiếng
            slotEndDateTime = slotDateTime.plusHours(1);
        }

        // Thêm 5 phút buffer (du di) để không bị ngắt kết nối đúng 00:00
        LocalDateTime tokenExpiryTime = slotEndDateTime.plusMinutes(5);
        long secondsUntilExpiry = java.time.Duration.between(now, tokenExpiryTime).getSeconds();

        // Cấp tối thiểu 30 phút trong trường hợp người dùng vào quá trễ so với giờ kết
        // thúc (vẫn trong khoảng check DEV MODE)
        int expireSeconds = secondsUntilExpiry > 0 ? (int) secondsUntilExpiry : 1800;

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
                .expireSeconds(expireSeconds)
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
        if (!isParticipant)
            throw new AppException(ErrorCode.FORBIDDEN);

        // startedAt chỉ set khi người đầu tiên vào call (teacher hoặc learner đầu tiên
        // join)
        if (session.getStartedAt() == null) {
            session.setStartedAt(LocalDateTime.now());
        }
        
        // Chuyển sang đang diễn ra khi có người vào
        if (session.getStatus() == SessionStatus.SCHEDULED) {
            session.setStatus(SessionStatus.IN_PROGRESS);
        }
        
        sessionRepository.save(session);
    }

    // ── Leave (mark endedAt) ────────────────────────────────
    @Transactional
    public void markLeave(UUID sessionId) {
        User user = userService.getCurrentUser();
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        boolean isParticipant = session.getTeacher().getId().equals(user.getId())
                || session.getLearner().getId().equals(user.getId());
        if (!isParticipant)
            throw new AppException(ErrorCode.FORBIDDEN);

        // Ghi nhận thời gian leave của từng người
        boolean isTeacher = session.getTeacher().getId().equals(user.getId());
        if (isTeacher) {
            session.setTeacherLeftAt(LocalDateTime.now());
        } else {
            session.setLearnerLeftAt(LocalDateTime.now());
        }

        boolean bothLeft = session.getTeacherLeftAt() != null && session.getLearnerLeftAt() != null;
        boolean ranLongEnough = session.getStartedAt() != null
                && java.time.Duration.between(session.getStartedAt(), LocalDateTime.now()).toMinutes() >= 2;

        // Chỉ kết thúc session nếu cả hai cùng thoát hoặc một bên thoát sau khi phòng chạy đủ lâu
        if (bothLeft || (ranLongEnough && (session.getTeacherLeftAt() != null || session.getLearnerLeftAt() != null))) {
            session.setEndedAt(LocalDateTime.now());
            session.setStatus(SessionStatus.COMPLETED);
        }

        sessionRepository.save(session);
    }

    // ── Confirm Session (Release Funds to Teacher) ──────────
    @Transactional
    public void confirmSession(UUID sessionId) {
        User user = userService.getCurrentUser();
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        if (!session.getLearner().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        if (session.getStatus() != SessionStatus.COMPLETED) {
            throw new AppException(ErrorCode.INVALID_REQUEST); // Only COMPLETED sessions can be confirmed
        }

        // Check if already paid
        boolean alreadyPaid = transactionRepository.existsByReferenceIdAndTransactionType(sessionId,
                TransactionType.EARN_SESSION);
        if (alreadyPaid)
            return;

        User teacher = session.getTeacher();
        teacher.setCreditsBalance(
                (teacher.getCreditsBalance() != null ? teacher.getCreditsBalance() : 0) + session.getCreditCost());
        userRepository.save(teacher);

        CreditTransaction tx = CreditTransaction.builder()
                .user(teacher)
                .amount(session.getCreditCost())
                .transactionType(TransactionType.EARN_SESSION)
                .referenceId(session.getId())
                .description("Earned from session " + session.getVideoRoomId())
                .build();
        transactionRepository.save(tx);
    }

    // ── Admin Escrow Management ─────────────────────────────
    @Transactional
    public void resolveDisputeRefundLearner(UUID sessionId, String adminNotes) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        if (session.getStatus() != SessionStatus.DISPUTED) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Refund the learner
        User learner = session.getLearner();
        learner.setCreditsBalance(
                (learner.getCreditsBalance() != null ? learner.getCreditsBalance() : 0) + session.getCreditCost());
        userRepository.save(learner);

        CreditTransaction tx = CreditTransaction.builder()
                .user(learner)
                .amount(session.getCreditCost())
                .transactionType(TransactionType.REFUND)
                .referenceId(session.getId())
                .description("Refund for disputed session " + session.getVideoRoomId() + ". Notes: " + adminNotes)
                .build();
        transactionRepository.save(tx);

        session.setStatus(SessionStatus.CANCELLED);
        sessionRepository.save(session);
        systemLogService.logSystemEvent("Phán xử tranh chấp: Hoàn tiền " + session.getCreditCost() + " Credits cho học viên (" + learner.getEmail() + ") của session: " + session.getVideoRoomId(), com.skillsync.skillsync.enums.LogLevel.INFO);
    }

    @Transactional
    public void resolveDisputeReleaseToMentor(UUID sessionId, String adminNotes) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        if (session.getStatus() != SessionStatus.DISPUTED) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Pay the teacher
        User teacher = session.getTeacher();
        teacher.setCreditsBalance(
                (teacher.getCreditsBalance() != null ? teacher.getCreditsBalance() : 0) + session.getCreditCost());
        userRepository.save(teacher);

        CreditTransaction tx = CreditTransaction.builder()
                .user(teacher)
                .amount(session.getCreditCost())
                .transactionType(TransactionType.EARN_SESSION)
                .referenceId(session.getId())
                .description(
                        "Released funds for disputed session " + session.getVideoRoomId() + ". Notes: " + adminNotes)
                .build();
        transactionRepository.save(tx);

        session.setStatus(SessionStatus.COMPLETED);
        sessionRepository.save(session);
        systemLogService.logSystemEvent("Phán xử tranh chấp: Trả " + session.getCreditCost() + " Credits cho Mentor (" + teacher.getEmail() + ") của session: " + session.getVideoRoomId(), com.skillsync.skillsync.enums.LogLevel.INFO);
    }

    // ── Map ─────────────────────────────────────────────────
    private SessionResponse toResponse(Session s) {
        // Extract review (if rating exists)
        Integer rating = null;
        String reviewText = null;
        if (s.getReviews() != null && !s.getReviews().isEmpty()) {
            // Priority: Get review from learner
            var learnerReview = s.getReviews().stream()
                    .filter(r -> r.getReviewer().getId().equals(s.getLearner().getId()))
                    .findFirst()
                    .orElse(s.getReviews().get(0));
            rating = learnerReview.getRating();
            reviewText = learnerReview.getComment();
        }

        return SessionResponse.builder()
                .id(s.getId())
                .videoRoomId(s.getVideoRoomId())
                .videoProvider(s.getVideoProvider())
                .status(s.getStatus())
                .creditCost(s.getCreditCost())
                .slotDate(s.getSlot() != null ? s.getSlot().getSlotDate() : null)
                .slotTime(s.getSlot() != null ? s.getSlot().getSlotTime() : null)
                .slotEndTime(s.getSlot() != null ? s.getSlot().getSlotEndTime() : null)
                .teacherId(s.getTeacher() != null ? s.getTeacher().getId() : null)
                .teacherName(s.getTeacher() != null ? s.getTeacher().getFullName() : null)
                .teacherAvatar(s.getTeacher() != null ? s.getTeacher().getAvatarUrl() : null)
                .learnerId(s.getLearner() != null ? s.getLearner().getId() : null)
                .learnerName(s.getLearner() != null ? s.getLearner().getFullName() : null)
                .teachingSkillId(s.getTeachingSkill() != null ? s.getTeachingSkill().getId() : null)
                .skillName(s.getTeachingSkill() != null ? s.getTeachingSkill().getSkill().getName() : null)
                .skillIcon(s.getTeachingSkill() != null ? s.getTeachingSkill().getSkill().getIcon() : null)
                .skillLevel(s.getTeachingSkill() != null ? s.getTeachingSkill().getLevel().toString() : null)
                .learnerNotes(s.getLearnerNotes())
                .startedAt(s.getStartedAt())
                .endedAt(s.getEndedAt())
                .createdAt(s.getCreatedAt())
                .rating(rating)
                .review(reviewText)
                .isPaid(transactionRepository.existsByReferenceIdAndTransactionType(s.getId(), TransactionType.EARN_SESSION))
                .isReported(sessionReportRepository.existsBySessionId(s.getId()))
                .build();
    }

    private SessionStatus parseStatus(String status) {
        if (status == null || status.isBlank())
            return null;
        try {
            return SessionStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ── Cancel (Learner hủy trước khi Approve) ─────────────
    @Transactional
    public void cancelSession(UUID sessionId) {
        User learner = userService.getCurrentUser();
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        // Chỉ learner của session mới được hủy
        if (!session.getLearner().getId().equals(learner.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        // Chỉ hủy được khi còn PENDING_APPROVAL
        if (session.getStatus() != SessionStatus.PENDING_APPROVAL) {
            throw new AppException(ErrorCode.SESSION_ALREADY_DECIDED);
        }

        session.setStatus(SessionStatus.CANCELLED);
        sessionRepository.save(session);

        // Nếu slot là PENDING (do propose), giải phóng lại
        TeachingSlot slot = session.getSlot();
        if (slot != null && slot.getStatus() == SlotStatus.PENDING) {
            slot.setStatus(SlotStatus.AVAILABLE);
            slotRepository.save(slot);
        }

        // Thông báo cho Mentor
        notificationService.createAndSend(NotificationCreateRequest.builder()
                .userId(session.getTeacher().getId())
                .type(NotificationType.SESSION_CANCELLED)
                .title("Học viên đã hủy yêu cầu")
                .content(learner.getFullName() + " đã hủy yêu cầu đặt lịch trước khi bạn phê duyệt.")
                .entityId(session.getId())
                .redirectUrl("/app/teaching")
                .build());
    }
}
