package com.skillsync.skillsync.service;

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

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class AuthService {
    final JwtService jwtService;
    final UserMapper userMapper;
    final UserRepository userRepository;
    final PasswordEncoder passwordEncoder;

    @Value("${google.client-id:}")
    String googleClientId;

    public AuthenticationResponse register(AuthenticationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.USER_EXISTS);
        }

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);

        userRepository.save(user);
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

    public AuthenticationResponse googleLogin(String idToken) {
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
                        newUser.setEmail(email);
                        newUser.setPassword("");
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

    private static boolean looksLikeJwt(String token) {
        String[] parts = token.split("\\.");
        return parts.length == 3
                && !parts[0].isBlank()
                && !parts[1].isBlank()
                && !parts[2].isBlank();
    }

    AuthenticationResponse buildAuth(User user) {
        return AuthenticationResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .userId(user.getId() != null ? user.getId().toString() : null)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
