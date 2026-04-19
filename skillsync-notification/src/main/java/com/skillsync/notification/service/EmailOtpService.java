package com.skillsync.notification.service;

import com.skillsync.notification.dto.request.TemplateEmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailOtpService {

    private final PasswordEncoder passwordEncoder;
    private final RestClient backendRestClient;
    private final EmailService emailService;

    private static final Random RNG = new Random();

    public boolean sendEmailVerificationOtp(String email, String recipientName) {
        String e = email != null ? email.trim() : "";
        if (e.isBlank()) return false;

        String code = generate6DigitCode();
        String codeHash = passwordEncoder.encode(code);
        String expiresAt = LocalDateTime.now().plusMinutes(10).toString();

        try {
            backendRestClient.post()
                    .uri("/auth/internal/email-verification/init")
                    .body(Map.of(
                            "email", e,
                            "codeHash", codeHash,
                            "expiresAt", expiresAt
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("[EmailOtpService] Failed to init OTP state: {}", ex.getMessage());
            return false;
        }

        Map<String, Object> variables = Map.of(
                "recipientName", recipientName != null && !recipientName.isBlank() ? recipientName : "there",
                "recipientEmail", e,
                "verificationCode", code
        );

        boolean sent = emailService.trySendHtmlEmail(new TemplateEmailRequest(
                e,
                "Verify your email address",
                "verify_email",
                variables
        ));
        if (!sent) {
            log.warn("[EmailOtpService] Verification OTP email not sent to {}", e);
        }
        return sent;
    }

    public boolean verifyEmailOtp(String email, String code) {
        String e = email != null ? email.trim() : "";
        String c = code != null ? code.trim() : "";
        if (e.isBlank() || !c.matches("^\\d{6}$")) return false;

        Map state;
        try {
            // ApiResponse unwrap happens only in FE; here we get raw object
            state = backendRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/auth/internal/email-verification/state")
                            .queryParam("email", e)
                            .build())
                    .retrieve()
                    .body(Map.class);
        } catch (Exception ex) {
            log.warn("[EmailOtpService] Failed to fetch OTP state: {}", ex.getMessage());
            return false;
        }

        Object dataObj = state != null ? state.get("data") : null;
        if (!(dataObj instanceof Map data)) return false;

        Object verifiedObj = data.get("emailVerified");
        if (Boolean.TRUE.equals(verifiedObj)) return true;

        String codeHash = data.get("codeHash") != null ? String.valueOf(data.get("codeHash")) : null;
        String expiresAtRaw = data.get("expiresAt") != null ? String.valueOf(data.get("expiresAt")) : null;
        if (codeHash == null || codeHash.isBlank() || expiresAtRaw == null || expiresAtRaw.isBlank()) return false;

        LocalDateTime expiresAt;
        try {
            expiresAt = LocalDateTime.parse(expiresAtRaw);
        } catch (Exception ex) {
            return false;
        }
        if (expiresAt.isBefore(LocalDateTime.now())) return false;
        if (!passwordEncoder.matches(c, codeHash)) return false;

        try {
            backendRestClient.post()
                    .uri("/auth/internal/email-verification/confirm")
                    .body(Map.of("email", e))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception ex) {
            log.warn("[EmailOtpService] Failed to confirm verification: {}", ex.getMessage());
            return false;
        }
    }

    public boolean sendPasswordResetOtp(String email) {
        String e = email != null ? email.trim() : "";
        if (e.isBlank()) return false;

        String code = generate6DigitCode();
        String codeHash = passwordEncoder.encode(code);
        String expiresAt = LocalDateTime.now().plusMinutes(10).toString();

        try {
            backendRestClient.post()
                    .uri("/auth/internal/password-reset/init")
                    .body(Map.of(
                            "email", e,
                            "codeHash", codeHash,
                            "expiresAt", expiresAt
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("[EmailOtpService] Failed to init password reset OTP state: {}", ex.getMessage());
            return false;
        }

        boolean sent = emailService.trySendHtmlEmail(new TemplateEmailRequest(
                e,
                "Password reset code",
                "reset_password",
                Map.of(
                        "recipientName", "there",
                        "recipientEmail", e,
                        "resetCode", code
                )
        ));
        if (!sent) {
            log.warn("[EmailOtpService] Password reset OTP email not sent to {}", e);
        }
        return sent;
    }

    public boolean resetPasswordWithOtp(String email, String code, String newPassword) {
        String e = email != null ? email.trim() : "";
        String c = code != null ? code.trim() : "";
        String np = newPassword != null ? newPassword : "";
        if (e.isBlank() || !c.matches("^\\d{6}$") || np.length() < 8) return false;

        Map state;
        try {
            state = backendRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/auth/internal/password-reset/state")
                            .queryParam("email", e)
                            .build())
                    .retrieve()
                    .body(Map.class);
        } catch (Exception ex) {
            log.warn("[EmailOtpService] Failed to fetch password reset OTP state: {}", ex.getMessage());
            return false;
        }

        Object dataObj = state != null ? state.get("data") : null;
        if (!(dataObj instanceof Map data)) return false;

        String codeHash = data.get("codeHash") != null ? String.valueOf(data.get("codeHash")) : null;
        String expiresAtRaw = data.get("expiresAt") != null ? String.valueOf(data.get("expiresAt")) : null;
        if (codeHash == null || codeHash.isBlank() || expiresAtRaw == null || expiresAtRaw.isBlank()) return false;

        LocalDateTime expiresAt;
        try {
            expiresAt = LocalDateTime.parse(expiresAtRaw);
        } catch (Exception ex) {
            return false;
        }
        if (expiresAt.isBefore(LocalDateTime.now())) return false;
        if (!passwordEncoder.matches(c, codeHash)) return false;

        try {
            backendRestClient.post()
                    .uri("/auth/internal/password-reset/apply")
                    .body(Map.of("email", e, "newPassword", np))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception ex) {
            log.warn("[EmailOtpService] Failed to apply password reset: {}", ex.getMessage());
            return false;
        }
    }

    public boolean verifyPasswordResetOtp(String email, String code) {
        String e = email != null ? email.trim() : "";
        String c = code != null ? code.trim() : "";
        if (e.isBlank() || !c.matches("^\\d{6}$")) return false;

        Map state;
        try {
            state = backendRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/auth/internal/password-reset/state")
                            .queryParam("email", e)
                            .build())
                    .retrieve()
                    .body(Map.class);
        } catch (Exception ex) {
            log.warn("[EmailOtpService] Failed to fetch password reset OTP state: {}", ex.getMessage());
            return false;
        }

        Object dataObj = state != null ? state.get("data") : null;
        if (!(dataObj instanceof Map data)) return false;

        String codeHash = data.get("codeHash") != null ? String.valueOf(data.get("codeHash")) : null;
        String expiresAtRaw = data.get("expiresAt") != null ? String.valueOf(data.get("expiresAt")) : null;
        if (codeHash == null || codeHash.isBlank() || expiresAtRaw == null || expiresAtRaw.isBlank()) return false;

        LocalDateTime expiresAt;
        try {
            expiresAt = LocalDateTime.parse(expiresAtRaw);
        } catch (Exception ex) {
            return false;
        }
        if (expiresAt.isBefore(LocalDateTime.now())) return false;

        return passwordEncoder.matches(c, codeHash);
    }

    private static String generate6DigitCode() {
        int value = RNG.nextInt(1_000_000);
        return String.format("%06d", value);
    }
}

