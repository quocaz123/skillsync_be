package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.common.ApiResponse;
import com.skillsync.skillsync.dto.request.session.BookSessionRequest;
import com.skillsync.skillsync.dto.request.session.ProposeSessionRequest;
import com.skillsync.skillsync.dto.response.session.SessionResponse;
import com.skillsync.skillsync.dto.response.session.ZegoTokenResponse;
import com.skillsync.skillsync.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    /**
     * POST /api/sessions/book
     * Learner đặt lịch → tạo Session + videoRoomId (PENDING_APPROVAL), chưa trừ credits.
     */
    @PostMapping("/book")
    public ApiResponse<SessionResponse> book(@RequestBody BookSessionRequest request) {
        return ApiResponse.success(sessionService.book(request));
    }

    /**
     * POST /api/sessions/propose
     * Learner tự đề xuất lịch học (On-Demand Booking)
     */
    @PostMapping("/propose")
    public ApiResponse<SessionResponse> propose(@RequestBody ProposeSessionRequest request) {
        return ApiResponse.success(sessionService.proposeSession(request));
    }

    /**
     * POST /api/sessions/{id}/approve
     * Teacher duyệt lịch → duyệt, trừ credits, tạo transaction, huỷ các PENDING khác
     */
    @PostMapping("/{id}/approve")
    public ApiResponse<SessionResponse> approveSession(@PathVariable UUID id) {
        return ApiResponse.success(sessionService.approveSession(id));
    }

    /**
     * POST /api/sessions/{id}/reject
     * Teacher từ chối lịch → huỷ
     */
    @PostMapping("/{id}/reject")
    public ApiResponse<Void> rejectSession(@PathVariable UUID id) {
        sessionService.rejectSession(id);
        return ApiResponse.success(null);
    }

    /**
     * GET /api/sessions/mine?role=learner|teacher|all&status=SCHEDULED|COMPLETED|CANCELLED
     * Trả danh sách sessions của user hiện tại.
     */
    @GetMapping("/mine")
    public ApiResponse<List<SessionResponse>> getMySessions(
            @RequestParam(required = false, defaultValue = "all") String role,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(sessionService.getMySessions(role, status));
    }

    /**
     * GET /api/sessions/{id}/zego-token
     * Trả ZEGO token cho teacher hoặc learner của session.
     * Chỉ cấp trong cửa sổ -10min đến +2h so với giờ học.
     */
    @GetMapping("/{id}/zego-token")
    public ApiResponse<ZegoTokenResponse> getZegoToken(@PathVariable UUID id) {
        return ApiResponse.success(sessionService.getZegoToken(id));
    }

    /**
     * POST /api/sessions/{id}/join
     * Frontend gọi sau khi ZEGO UIKit đã mount thành công.
     * Đặt startedAt (chỉ lần đầu).
     */
    @PostMapping("/{id}/join")
    public ApiResponse<Void> join(@PathVariable UUID id) {
        sessionService.markJoin(id);
        return ApiResponse.success(null);
    }

    /**
     * POST /api/sessions/{id}/leave
     * Frontend gọi khi user bấm "Kết thúc".
     * Đặt endedAt + status = COMPLETED.
     */
    @PostMapping("/{id}/leave")
    public ApiResponse<Void> leave(@PathVariable UUID id) {
        sessionService.markLeave(id);
        return ApiResponse.success(null);
    }

    /**
     * POST /api/sessions/{id}/confirm
     * Learner xác nhận hoàn thành buổi học, giải phóng tiền cho Teacher.
     */
    @PostMapping("/{id}/confirm")
    public ApiResponse<Void> confirm(@PathVariable UUID id) {
        sessionService.confirmSession(id);
        return ApiResponse.success(null);
    }

    /**
     * PATCH /api/sessions/{id}/cancel
     * Learner hủy session khi còn ở trạng thái PENDING_APPROVAL.
     * Credits chưa bị trừ nên chỉ cần chuyển status sang CANCELLED.
     */
    @PatchMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable UUID id) {
        sessionService.cancelSession(id);
        return ApiResponse.success(null);
    }
}
