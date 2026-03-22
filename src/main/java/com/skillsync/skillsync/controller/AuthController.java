package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.constant.CookieNames;
import com.skillsync.skillsync.dto.common.ApiResponse;
import com.skillsync.skillsync.dto.request.auth.AuthenticationRequest;
import com.skillsync.skillsync.dto.request.auth.GoggleLoginRequest;
import com.skillsync.skillsync.dto.request.auth.LoginRequest;
import com.skillsync.skillsync.dto.response.auth.AuthenticationResponse;
import com.skillsync.skillsync.dto.response.auth.UserAuthResponse;
import com.skillsync.skillsync.exception.AppException;
import com.skillsync.skillsync.exception.ErrorCode;
import com.skillsync.skillsync.service.AuthService;
import com.skillsync.skillsync.service.CookieService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final CookieService cookieService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserAuthResponse>> register(@RequestBody AuthenticationRequest request,
            HttpServletResponse response) {
        AuthenticationResponse auth = authService.register(request);
        cookieService.setAuthCookies(response, auth);
        
        UserAuthResponse userAuthResponse = mapToUserAuthResponse(auth);
        return ResponseEntity.ok(ApiResponse.success(userAuthResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserAuthResponse>> login(@RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthenticationResponse auth = authService.login(request);
        cookieService.setAuthCookies(response, auth);

        UserAuthResponse userAuthResponse = mapToUserAuthResponse(auth);
        return ResponseEntity.ok(ApiResponse.success(userAuthResponse));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<UserAuthResponse>> googleLogin(@RequestBody GoggleLoginRequest request,
            HttpServletResponse response) {
        AuthenticationResponse auth = authService.googleLogin(request.getIdToken());
        cookieService.setAuthCookies(response, auth);

        UserAuthResponse userAuthResponse = mapToUserAuthResponse(auth);
        return ResponseEntity.ok(ApiResponse.success(userAuthResponse));
    }
    

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<UserAuthResponse>> refresh(
        HttpServletRequest request,
        HttpServletResponse response) {

    String refreshToken = null;

    if (request.getCookies() != null) {
        for (Cookie c : request.getCookies()) {
            if (CookieNames.REFRESH_TOKEN.equals(c.getName())) {
                refreshToken = c.getValue();
                break;
            }
        }
    }

    if (refreshToken == null) {
        throw new AppException(ErrorCode.NO_REFRESH_TOKEN);
    }

    AuthenticationResponse auth = authService.refresh(refreshToken);
    cookieService.setAuthCookies(response, auth);

    UserAuthResponse userAuthResponse = mapToUserAuthResponse(auth);
    return ResponseEntity.ok(ApiResponse.success(userAuthResponse));
}

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        cookieService.clearAuthCookies(response);
        return ResponseEntity.ok(new ApiResponse<>(200, "Logged out successfully", null));
    }

    private UserAuthResponse mapToUserAuthResponse(AuthenticationResponse auth) {
        return UserAuthResponse.builder()
                .userId(auth.getUserId())
                .email(auth.getEmail())
                .role(auth.getRole())
                .build();
    }
}

