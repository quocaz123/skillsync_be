package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.constant.CookieNames;
import com.skillsync.skillsync.dto.common.ApiResponse;
import com.skillsync.skillsync.dto.request.auth.AuthenticationRequest;
import com.skillsync.skillsync.dto.request.auth.ForgotPasswordRequest;
import com.skillsync.skillsync.dto.request.auth.GoogleCodeExchangeRequest;
import com.skillsync.skillsync.dto.request.auth.InternalInitPasswordResetOtpRequest;
import com.skillsync.skillsync.dto.request.auth.InternalSetEmailVerificationRequest;
import com.skillsync.skillsync.dto.request.auth.LoginRequest;
import com.skillsync.skillsync.dto.request.auth.ResetPasswordRequest;
import com.skillsync.skillsync.dto.response.auth.AuthenticationResponse;
import com.skillsync.skillsync.dto.response.auth.EmailVerificationStateResponse;
import com.skillsync.skillsync.dto.response.auth.PasswordResetOtpStateResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final CookieService cookieService;

    @PostMapping("/register")
    public ApiResponse<UserAuthResponse> register(@RequestBody AuthenticationRequest request,
            HttpServletResponse response) {
        AuthenticationResponse auth = authService.register(request);
        // Do NOT set auth cookies here — user must log in manually after registration

        UserAuthResponse userAuthResponse = mapToUserAuthResponse(auth);
        return ApiResponse.success(userAuthResponse);
    }

    @PostMapping("/login")
    public ApiResponse<UserAuthResponse> login(@RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthenticationResponse auth = authService.login(request);
        cookieService.setAuthCookies(response, auth);

        UserAuthResponse userAuthResponse = mapToUserAuthResponse(auth);
        return ApiResponse.success(userAuthResponse);
    }


    @PostMapping("/google/exchange")
    public ApiResponse<UserAuthResponse> googleExchangeCode(@RequestBody GoogleCodeExchangeRequest request,
            HttpServletResponse response) {
        AuthenticationResponse auth = authService.googleExchangeCode(request.getCode(), request.getRedirectUri());
        cookieService.setAuthCookies(response, auth);

        UserAuthResponse userAuthResponse = mapToUserAuthResponse(auth);
        return ApiResponse.success(userAuthResponse);
    }
    

    @PostMapping("/refresh")
    public ApiResponse<UserAuthResponse> refresh(
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
    return ApiResponse.success(userAuthResponse);
}

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        cookieService.clearAuthCookies(response);
        return ApiResponse.success(null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success(null);
    }

    // Internal endpoints used by skillsync-notification to manage OTP state.
    @PostMapping("/internal/email-verification/init")
    public ApiResponse<Void> internalInitEmailVerification(@RequestBody InternalSetEmailVerificationRequest request) {
        authService.internalInitEmailVerification(request);
        return ApiResponse.success(null);
    }

    @GetMapping("/internal/email-verification/state")
    public ApiResponse<EmailVerificationStateResponse> internalGetEmailVerificationState(@RequestParam("email") String email) {
        return ApiResponse.success(authService.internalGetEmailVerificationState(email));
    }

    @PostMapping("/internal/email-verification/confirm")
    public ApiResponse<Void> internalConfirmEmailVerification(@RequestBody ForgotPasswordRequest request) {
        authService.internalConfirmEmailVerified(request != null ? request.getEmail() : null);
        return ApiResponse.success(null);
    }

    @PostMapping("/internal/password-reset/init")
    public ApiResponse<Void> internalInitPasswordReset(@RequestBody InternalInitPasswordResetOtpRequest request) {
        authService.internalInitPasswordResetOtp(request);
        return ApiResponse.success(null);
    }

    @GetMapping("/internal/password-reset/state")
    public ApiResponse<PasswordResetOtpStateResponse> internalGetPasswordResetState(@RequestParam("email") String email) {
        return ApiResponse.success(authService.internalGetPasswordResetOtpState(email));
    }

    @PostMapping("/internal/password-reset/apply")
    public ApiResponse<Void> internalApplyPasswordReset(@RequestBody Map<String, String> body) {
        String email = body != null ? body.get("email") : null;
        String newPassword = body != null ? body.get("newPassword") : null;
        authService.internalApplyPasswordReset(email, newPassword);
        return ApiResponse.success(null);
    }

    private UserAuthResponse mapToUserAuthResponse(AuthenticationResponse auth) {
        return UserAuthResponse.builder()
                .userId(auth.getUserId())
                .email(auth.getEmail())
                .fullName(auth.getFullName())
                .role(auth.getRole())
                .avatarUrl(auth.getAvatarUrl())
                .hasPassword(auth.getHasPassword())
                .isFirstLogin(auth.getIsFirstLogin())
                .build();
    }
}

