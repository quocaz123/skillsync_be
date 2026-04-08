package com.skillsync.skillsync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.openidconnect.IdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.skillsync.skillsync.dto.request.auth.AuthenticationRequest;
import com.skillsync.skillsync.dto.request.auth.LoginRequest;
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
import java.util.Collections;
import java.util.Map;

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

        // Publish WELCOME email event lên Kafka → skillsync-notification sẽ gửi email
        notificationEventPublisher.publishWelcome(user.getEmail(), user.getFullName());
        log.info("[AuthService] Published WELCOME event for new user: {}", user.getEmail());

        return buildAuth(user);
    }

    public AuthenticationResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

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
                        newUser.setRole(Role.USER);
                        log.info("New user created from Google login: {}", email);
                        return userRepository.save(newUser);
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
