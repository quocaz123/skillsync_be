package com.skillsync.skillsync.service;

import com.skillsync.skillsync.exception.AppException;
import com.skillsync.skillsync.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * ZEGO Token04 server-side token generation.
 * Uses AES-256-GCM authenticated encryption (NOT AES-128-CBC).
 * Reference: https://github.com/zegoim/zego_server_assistant
 */
@Service
@Slf4j
public class ZegoTokenService {

    @Value("${zego.app-id}")
    private Long appId;

    @Value("${zego.server-secret}")
    private String serverSecret;

    private static final long DEFAULT_EXPIRE_SECONDS = 3600; // 1 hour
    private static final int GCM_TAG_LENGTH = 128; // 16 bytes in bits
    private static final int NONCE_LENGTH = 12; // 12 bytes for GCM

    public Long getAppId() {
        return appId;
    }

    /**
     * Generate a ZEGO Token04 using AES-256-GCM or AES-128-GCM depending on secret
     * length.
     * Supports both 32-char (16 bytes for AES-128) and 64-char (32 bytes for
     * AES-256) hex secrets.
     *
     * @param roomId   ZEGO room ID (= session.videoRoomId)
     * @param userId   string user ID
     * @param userName display name shown inside the call
     * @return signed token string (prefixed with "04")
     */
    public String generateToken(String roomId, String userId, String userName) {
        if (serverSecret == null || serverSecret.isBlank() || serverSecret.length() < 32) {
            log.error(
                    "zego.server-secret is not configured properly. Set environment variable ZEGO_SERVER_SECRET (32 or 64 hex chars from console.zegocloud.com)");
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        if (serverSecret.equals("00000000000000000000000000000000")
                || serverSecret.equals("0000000000000000000000000000000000000000000000000000000000000000")) {
            log.error(
                    "zego.server-secret is still using dummy value. Please set ZEGO_SERVER_SECRET environment variable from console.zegocloud.com");
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        try {
            long expireTime = System.currentTimeMillis() / 1000 + DEFAULT_EXPIRE_SECONDS;

            // Payload: JSON string
            String payload = String.format(
                    "{\"room_id\":\"%s\",\"user_id\":\"%s\",\"user_name\":\"%s\",\"exp\":%d}",
                    roomId, userId, userName, expireTime);

            // Determine key size based on secret length
            byte[] key;
            int keySize;
            if (serverSecret.length() >= 64) {
                // AES-256-GCM with 32-byte key
                key = hexStringToByteArray(serverSecret.substring(0, 64));
                keySize = 256;
                log.debug("Using AES-256-GCM for token generation (64-char secret)");
            } else {
                // AES-128-GCM with 16-byte key (32-char secret)
                key = hexStringToByteArray(serverSecret.substring(0, 32));
                keySize = 128;
                log.debug("Using AES-128-GCM for token generation (32-char secret)");
            }

            // Generate random 12-byte nonce
            SecureRandom random = new SecureRandom();
            byte[] nonce = new byte[NONCE_LENGTH];
            random.nextBytes(nonce);

            // AES-GCM encryption
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), gcmSpec);
            byte[] encryptedPayload = cipher.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            // Build binary token structure (BigEndian):
            // [0-7] expire(int64)
            // [8-9] nonce_len(uint16)
            // [10-21] nonce(12 bytes)
            // [22-23] encrypted_len(uint16)
            // [24+] encrypted_data + auth_tag
            // [last] mode_byte(0x01 for GCM)

            int totalSize = 8 + 2 + NONCE_LENGTH + 2 + encryptedPayload.length + 1;
            ByteBuffer buf = ByteBuffer.allocate(totalSize);
            buf.order(ByteOrder.BIG_ENDIAN);

            buf.putLong(expireTime);
            buf.putShort((short) NONCE_LENGTH);
            buf.put(nonce);
            buf.putShort((short) encryptedPayload.length);
            buf.put(encryptedPayload);
            buf.put((byte) 0x01); // GCM mode

            // Token format: "04" + base64(buffer)
            String encodedPayload = Base64.getEncoder().encodeToString(buf.array());
            return "04" + encodedPayload;
        } catch (Exception e) {
            log.error("ZEGO token generation failed", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Convert hex string to byte array (for server secret).
     * Expects 64 hex characters (32 bytes for AES-256).
     */
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

}