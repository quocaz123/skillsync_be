package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.response.ApiResponse;
import com.skillsync.skillsync.dto.response.session.SessionResponse;
import com.skillsync.skillsync.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/escrow")
@RequiredArgsConstructor
public class AdminEscrowController {

    private final SessionService sessionService;

    @GetMapping
    public ApiResponse<List<SessionResponse>> getEscrowSessions() {
        return ApiResponse.<List<SessionResponse>>builder()
                .result(sessionService.getEscrowSessions())
                .build();
    }
}
