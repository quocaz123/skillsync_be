package com.skillsync.notification.controller;

import com.skillsync.notification.dto.request.ForgotPasswordRequest;
import com.skillsync.notification.service.EmailOtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PasswordResetController {

    private final RestClient backendRestClient;
    private final EmailOtpService emailOtpService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostMapping("/password/forgot")
    public Map<String, Object> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            String email = request != null ? request.getEmail() : null;
            boolean sent = emailOtpService.sendPasswordResetOtp(email);
            return Map.of("ok", sent);
        } catch (Exception e) {
            // Intentionally return success to avoid leaking system state.
            log.warn("[PasswordResetController] forgotPassword proxy failed: {}", e.getMessage());
        }

        return Map.of("ok", false);
    }

    @PostMapping("/otp/send")
    public Map<String, Object> sendOtp(@RequestBody ForgotPasswordRequest request) {
        // We reuse DTO with `email` field.
        try {
            String email = request != null ? request.getEmail() : null;
            // Try to resolve name from backend (optional). We'll just send with a generic name.
            emailOtpService.sendEmailVerificationOtp(email, "there");
        } catch (Exception e) {
            // Intentionally return success
        }
        return Map.of("ok", true);
    }

    @PostMapping("/otp/verify")
    public Map<String, Object> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body != null ? body.get("email") : null;
        String code = body != null ? body.get("code") : null;
        boolean ok = emailOtpService.verifyEmailOtp(email, code);
        return Map.of("ok", ok);
    }

    @PostMapping("/password/reset-otp")
    public Map<String, Object> resetPasswordOtp(@RequestBody Map<String, String> body) {
        String email = body != null ? body.get("email") : null;
        String code = body != null ? body.get("code") : null;
        String newPassword = body != null ? body.get("newPassword") : null;
        boolean ok = emailOtpService.resetPasswordWithOtp(email, code, newPassword);
        return Map.of("ok", ok);
    }

    @PostMapping("/password/verify-otp")
    public Map<String, Object> verifyPasswordOtp(@RequestBody Map<String, String> body) {
        String email = body != null ? body.get("email") : null;
        String code = body != null ? body.get("code") : null;
        boolean ok = emailOtpService.verifyPasswordResetOtp(email, code);
        return Map.of("ok", ok);
    }

    @GetMapping("/password/reset")
    public ModelAndView resetForm(@RequestParam("token") String token) {
        return new ModelAndView("reset_form", Map.of(
                "token", token,
                "frontendUrl", frontendUrl
        ));
    }

    @PostMapping(value = "/password/reset", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ModelAndView resetSubmit(@RequestParam("token") String token,
                                    @RequestParam("newPassword") String newPassword,
                                    @RequestParam("confirmPassword") String confirmPassword) {

        if (token == null || token.isBlank()) {
            return new ModelAndView("reset_form", Map.of(
                    "token", token,
                    "frontendUrl", frontendUrl,
                    "error", "Invalid reset token."
            ));
        }
        if (newPassword == null || newPassword.length() < 8) {
            return new ModelAndView("reset_form", Map.of(
                    "token", token,
                    "frontendUrl", frontendUrl,
                    "error", "Password must be at least 8 characters."
            ));
        }
        if (!newPassword.equals(confirmPassword)) {
            return new ModelAndView("reset_form", Map.of(
                    "token", token,
                    "frontendUrl", frontendUrl,
                    "error", "Passwords do not match."
            ));
        }

        try {
            backendRestClient.post()
                    .uri("/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("token", token, "newPassword", newPassword))
                    .retrieve()
                    .toBodilessEntity();

            return new ModelAndView("reset_form", Map.of(
                    "token", token,
                    "frontendUrl", frontendUrl,
                    "success", "Your password has been updated. You can close this tab."
            ));
        } catch (Exception e) {
            log.warn("[PasswordResetController] resetPassword proxy failed: {}", e.getMessage());
            return new ModelAndView("reset_form", Map.of(
                    "token", token,
                    "frontendUrl", frontendUrl,
                    "error", "Reset failed. The link may be invalid or expired."
            ));
        }
    }
}

