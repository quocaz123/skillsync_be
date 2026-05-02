package com.skillsync.skillsync.service;

import com.skillsync.skillsync.entity.SystemLog;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.enums.LogLevel;
import com.skillsync.skillsync.repository.SystemLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;
    private final UserService userService;

    public void logSystemEvent(String action, LogLevel level) {
        try {
            User currentUser = null;
            try {
                currentUser = userService.getCurrentUser();
            } catch (Exception e) {
                // Ignore if not logged in
            }
            
            String ipAddress = getClientIpAddress();

            SystemLog systemLog = SystemLog.builder()
                    .user(currentUser)
                    .action(action)
                    .level(level)
                    .ipAddress(ipAddress)
                    .build();

            try {
                systemLogRepository.save(systemLog);
            } catch (Exception e) {
                log.error("Failed to save system log: ", e);
            }

        } catch (Exception e) {
            log.error("Error creating system log: ", e);
        }
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xfHeader = request.getHeader("X-Forwarded-For");
                if (xfHeader == null) {
                    return request.getRemoteAddr();
                }
                return xfHeader.split(",")[0];
            }
        } catch (Exception e) {
            // Ignore
        }
        return "Unknown";
    }
}
