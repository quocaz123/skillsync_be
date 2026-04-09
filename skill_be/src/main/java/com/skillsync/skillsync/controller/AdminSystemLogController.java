package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.common.ApiResponse;
import com.skillsync.skillsync.dto.response.admin.SystemLogResponse;
import com.skillsync.skillsync.entity.SystemLog;
import com.skillsync.skillsync.repository.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSystemLogController {

    private final SystemLogRepository systemLogRepository;

    @GetMapping
    public ApiResponse<Page<SystemLogResponse>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
            
        Pageable pageable = PageRequest.of(page, size);
        Page<SystemLog> logs = systemLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        
        Page<SystemLogResponse> responses = logs.map(log -> SystemLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .level(log.getLevel())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .userEmail(log.getUser() != null ? log.getUser().getEmail() : null)
                .userName(log.getUser() != null ? log.getUser().getFullName() : null)
                .build()
        );
        
        return ApiResponse.success(responses);
    }
}
