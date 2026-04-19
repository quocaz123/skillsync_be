package com.skillsync.skillsync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.openidconnect.IdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.skillsync.skillsync.dto.request.auth.AuthenticationRequest;
import com.skillsync.skillsync.dto.request.auth.ForgotPasswordRequest;
import com.skillsync.skillsync.dto.request.auth.InternalSetEmailVerificationRequest;
import com.skillsync.skillsync.dto.request.auth.InternalInitPasswordResetOtpRequest;
import com.skillsync.skillsync.dto.request.auth.LoginRequest;
import com.skillsync.skillsync.dto.request.auth.ResetPasswordRequest;
import com.skillsync.skillsync.dto.response.auth.EmailVerificationStateResponse;
import com.skillsync.skillsync.dto.response.auth.PasswordResetOtpStateResponse;
import com.skillsync.skillsync.dto.response.auth.AuthenticationResponse;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.enums.Role;
import com.skillsync.skillsync.exception.AppException;
import com.skillsync.skillsync.exception.ErrorCode;
import com.skillsync.skillsync.mapper.UserMapper;
import com.skillsync.skillsync.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class AuthService {
    final JwtService jwtService;
    final UserMapper userMapper;
    final UserRepository userRepository;
    final PasswordEncoder passwordEncoder;
    final ObjectMapper objectMapper;
    final NotificationEventPublisher notificationEventPublisher;

    @Value("${google.client-id:}")
    String googleClientId;

    @Value("${google.client-secret:}")
    String googleClientSecret;

    @Value("${app.notification.public-url}")
    String notificationPublicUrl;

    public AuthenticationResponse register(AuthenticationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.USER_EXISTS);
        }

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);

        // Set display name: use provided fullName, else derive from email
        String fullName = (request.getFullName() != null && !request.getFullName().isBlank())
                ? request.getFullName().trim()
                : request.getEmail().split("@")[0];
        user.setFullName(fullName);

        userRepository.save(user);

        // Request OTP generation/sending in skillsync-notification
        notificationEventPublisher.publishEmailVerificationRequest(user.getEmail(), user.getFullName());

        return buildAuth(user);
    }

    public AuthenticationResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        return buildAuth(user);
    }

    public AuthenticationResponse refresh(String refreshToken) {
        var claims = jwtService.parseToken(refreshToken);
        String email = claims.getSubject();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return buildAuth(user);
    }

    public void requestPasswordReset(ForgotPasswordRequest request) {
        String email = request != null && request.getEmail() != null ? request.getEmail().trim() : "";
        if (email.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        // Always return success to avoid user enumeration.
        if (userOpt.isEmpty()) {
            log.info("[AuthService] Password reset requested for non-existent email: {}", email);
            return;
        }

        User user = userOpt.get();
        String token = jwtService.generatePasswordResetToken(user.getEmail());
        String resetUrl = notificationPublicUrl + "/password/reset?token=" + enc(token);

        notificationEventPublisher.publishPasswordReset(user.getEmail(), user.getFullName(), resetUrl);
        log.info("[AuthService] Published PASSWORD_RESET event for {}", user.getEmail());
    }

    public void resetPassword(ResetPasswordRequest request) {
        String token = request != null && request.getToken() != null ? request.getToken().trim() : "";
        String newPassword = request != null && request.getNewPassword() != null ? request.getNewPassword() : "";
        if (token.isBlank() || newPassword.isBlank() || newPassword.length() < 8) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        try {
            var claims = jwtService.parseToken(token);
            Object type = claims.get("type");
            if (!"PASSWORD_RESET".equals(type)) {
                throw new AppException(ErrorCode.INVALID_TOKEN);
            }
            String email = claims.getSubject();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            user.setPassword(passwordEncoder.encode(newPassword));
            user.setHasPassword(true);
            userRepository.save(user);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
    }

    // OTP generation/verification is handled in skillsync-notification.

    private AuthenticationResponse googleLogin(String idToken) {
        String raw = idToken == null ? "" : idToken.trim();
        if (raw.isBlank()) {
            throw new AppException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }
        if (googleClientId == null || googleClientId.isBlank()) {
            log.error("google.client-id is not configured");
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        if (!looksLikeJwt(raw)) {
            log.warn(
                    "Google login rejected: expected JWT id_token (3 segments). Often caused by sending access_token or auth code instead of id_token.");
            throw new AppException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken token = verifier.verify(raw);

            if (token == null) {
                log.warn("Invalid Google ID token provided");
                throw new AppException(ErrorCode.INVALID_GOOGLE_TOKEN);
            }

            Payload payload = token.getPayload();
            String email = (String) payload.get("email");
            Boolean emailVerified = (Boolean) payload.get("email_verified");

            if (email == null || email.isBlank() || !Boolean.TRUE.equals(emailVerified)) {
                log.warn("Google token missing valid verified email");
                throw new AppException(ErrorCode.INVALID_GOOGLE_TOKEN);
            }

            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setFullName((String) payload.get("name"));
                        newUser.setEmail(email);
                        // Random password for Google-only users — hasPassword=false so FE can prompt them to set one
                        newUser.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
                        newUser.setHasPassword(false);
                        newUser.setEmailVerified(true);
                        newUser.setRole(Role.USER);
                        log.info("New user created from Google login: {}", email);
                        User saved = userRepository.save(newUser);

                        // Publish WELCOME email event cho Google-first users (tương tự register())
                        notificationEventPublisher.publishWelcome(saved.getEmail(), saved.getFullName());
                        log.info("[AuthService] Published WELCOME event for new Google user: {}", saved.getEmail());

                        return saved;
                    });

            return buildAuth(user);
        } catch (AppException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid Google id_token (malformed JWT or bad base64)", e);
            throw new AppException(ErrorCode.INVALID_GOOGLE_TOKEN);
        } catch (Exception e) {
            log.error("Error during Google login verification", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public AuthenticationResponse googleExchangeCode(String code, String redirectUri) {
        String codeValue = code == null ? "" : code.trim();
        String redirectUriValue = redirectUri == null ? "" : redirectUri.trim();
        if (codeValue.isBlank() || redirectUriValue.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (googleClientId == null || googleClientId.isBlank() || googleClientSecret == null || googleClientSecret.isBlank()) {
            log.error("Google OAuth config is missing (client-id/client-secret)");
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        try {
            String body = "code=" + enc(codeValue)
                    + "&client_id=" + enc(googleClientId)
                    + "&client_secret=" + enc(googleClientSecret)
                    + "&redirect_uri=" + enc(redirectUriValue)
                    + "&grant_type=authorization_code";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Google token exchange failed: status={}, body={}", response.statusCode(), response.body());
                throw new AppException(ErrorCode.INVALID_GOOGLE_TOKEN);
            }

            Map<String, Object> payload = objectMapper.readValue(response.body(), Map.class);
            Object idTokenObj = payload.get("id_token");
            if (!(idTokenObj instanceof String idToken) || idToken.isBlank()) {
                log.warn("Google token exchange response missing id_token");
                throw new AppException(ErrorCode.INVALID_GOOGLE_TOKEN);
            }

            return googleLogin(idToken);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error during Google auth code exchange", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private static boolean looksLikeJwt(String token) {
        String[] parts = token.split("\\.");
        return parts.length == 3
                && !parts[0].isBlank()
                && !parts[1].isBlank()
                && !parts[2].isBlank();
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public void internalInitEmailVerification(InternalSetEmailVerificationRequest request) {
        String email = request != null && request.getEmail() != null ? request.getEmail().trim() : "";
        String codeHash = request != null ? request.getCodeHash() : null;
        String expiresAtRaw = request != null ? request.getExpiresAt() : null;
        if (email.isBlank() || codeHash == null || codeHash.isBlank() || expiresAtRaw == null || expiresAtRaw.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        LocalDateTime expiresAt;
        try {
            expiresAt = LocalDateTime.parse(expiresAtRaw);
        } catch (DateTimeParseException e) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return;
        User user = userOpt.get();
        if (Boolean.TRUE.equals(user.getEmailVerified())) return;

        user.setEmailVerificationCodeHash(codeHash);
        user.setEmailVerificationExpiresAt(expiresAt);
        userRepository.save(user);
    }

    public EmailVerificationStateResponse internalGetEmailVerificationState(String emailRaw) {
        String email = emailRaw != null ? emailRaw.trim() : "";
        if (email.isBlank()) throw new AppException(ErrorCode.INVALID_REQUEST);

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return new EmailVerificationStateResponse(false, null, null);
        }
        User user = userOpt.get();
        return new EmailVerificationStateResponse(
                user.getEmailVerified() != null ? user.getEmailVerified() : false,
                user.getEmailVerificationCodeHash(),
                user.getEmailVerificationExpiresAt() != null ? user.getEmailVerificationExpiresAt().toString() : null
        );
    }

    public void internalConfirmEmailVerified(String emailRaw) {
        String email = emailRaw != null ? emailRaw.trim() : "";
        if (email.isBlank()) throw new AppException(ErrorCode.INVALID_REQUEST);

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return;
        User user = userOpt.get();

        user.setEmailVerified(true);
        user.setEmailVerificationCodeHash(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);
    }

    // ── Password reset OTP (handled by skillsync-notification) ───────────────

    public void internalInitPasswordResetOtp(InternalInitPasswordResetOtpRequest request) {
        String email = request != null && request.getEmail() != null ? request.getEmail().trim() : "";
        String codeHash = request != null ? request.getCodeHash() : null;
        String expiresAtRaw = request != null ? request.getExpiresAt() : null;
        if (email.isBlank() || codeHash == null || codeHash.isBlank() || expiresAtRaw == null || expiresAtRaw.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        LocalDateTime expiresAt;
        try {
            expiresAt = LocalDateTime.parse(expiresAtRaw);
        } catch (DateTimeParseException e) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return;
        User user = userOpt.get();
        user.setPasswordResetCodeHash(codeHash);
        user.setPasswordResetExpiresAt(expiresAt);
        userRepository.save(user);
    }

    public PasswordResetOtpStateResponse internalGetPasswordResetOtpState(String emailRaw) {
        String email = emailRaw != null ? emailRaw.trim() : "";
        if (email.isBlank()) throw new AppException(ErrorCode.INVALID_REQUEST);

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return new PasswordResetOtpStateResponse(null, null);
        User user = userOpt.get();
        return new PasswordResetOtpStateResponse(
                user.getPasswordResetCodeHash(),
                user.getPasswordResetExpiresAt() != null ? user.getPasswordResetExpiresAt().toString() : null
        );
    }

    public void internalApplyPasswordReset(String emailRaw, String newPassword) {
        String email = emailRaw != null ? emailRaw.trim() : "";
        if (email.isBlank() || newPassword == null || newPassword.length() < 8) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return;
        User user = userOpt.get();

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setHasPassword(true);
        user.setPasswordResetCodeHash(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);
    }

    AuthenticationResponse buildAuth(User user) {
        return AuthenticationResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .userId(user.getId() != null ? user.getId().toString() : null)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .avatarUrl(user.getAvatarUrl())
                .hasPassword(user.getHasPassword() != null ? user.getHasPassword() : true)
                .creditsBalance(user.getCreditsBalance())
                .build();
    }
}
