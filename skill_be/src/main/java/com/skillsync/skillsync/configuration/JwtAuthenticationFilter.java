package com.skillsync.skillsync.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.skillsync.constant.CookieNames;
import com.skillsync.skillsync.enums.UserStatus;
import com.skillsync.skillsync.repository.UserRepository;
import com.skillsync.skillsync.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            // Cookie-based auth (HttpOnly) cho các request từ FE qua XHR
            token = readCookie(request, CookieNames.ACCESS_TOKEN);
        }

        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.parseToken(token);
            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            if (email != null && role != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // ── Kiểm tra trạng thái ban ──────────────────────────────────
                var userOpt = userRepository.findByEmail(email);
                if (userOpt.isEmpty() || userOpt.get().getStatus() == UserStatus.BANNED) {
                    log.warn("Blocked request from banned/deleted user: {}", email);
                    sendBannedResponse(response);
                    return;
                }
                // ─────────────────────────────────────────────────────────────

                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                var auth = new UsernamePasswordAuthenticationToken(email, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (JwtException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /** Trả về HTTP 403 dạng JSON chuẩn khi user bị ban. */
    private void sendBannedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Xóa cookie để tránh trình duyệt tiếp tục gửi token của user bị ban
        clearAuthCookies(response);

        Map<String, Object> body = Map.of(
                "code", 403,
                "message", "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.",
                "success", false);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private void clearAuthCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie(CookieNames.ACCESS_TOKEN, null);
        accessCookie.setPath("/");
        accessCookie.setHttpOnly(true);
        accessCookie.setMaxAge(0);

        Cookie refreshCookie = new Cookie(CookieNames.REFRESH_TOKEN, null);
        refreshCookie.setPath("/skillsync/auth/refresh");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setMaxAge(0);

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
    }

    private static String readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
