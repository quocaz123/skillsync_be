package com.skillsync.skillsync.service;

import com.skillsync.skillsync.exception.AppException;
import com.skillsync.skillsync.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * ZEGO Token04 server-side token generation.
 * Reference: https://www.zegocloud.com/docs/server-and-client-brief/token-authentication
 * Token04 format: version(2) + expire(4) + nonce(8) + payload_len(4) + encrypted_payload
 * Encrypted with AES-128-CBC, key = first 16 bytes of ServerSecret, iv = first 16 bytes of expire+nonce
 */
@Service
@Slf4j
public class ZegoTokenService {

    @Value("${zego.app-id}")
    private Long appId;

    @Value("${zego.server-secret}")
    private String serverSecret;

    private static final long DEFAULT_EXPIRE_SECONDS = 3600; // 1 hour

    public Long getAppId() {
        return appId;
    }

    /**
     * Generate a ZEGO Token04.
     *
     * @param roomId   ZEGO room ID (= session.videoRoomId)
     * @param userId   string user ID
     * @param userName display name shown inside the call
     * @return signed token string
     */
    public String generateToken(String roomId, String userId, String userName) {
        if (serverSecret == null || serverSecret.isBlank()) {
            log.error("zego.server-secret is not configured");
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        try {
            long expireTime = System.currentTimeMillis() / 1000 + DEFAULT_EXPIRE_SECONDS;
            long nonce = (long) (Math.random() * Long.MAX_VALUE);

            // Payload: JSON string
            String payload = String.format(
                    "{\"room_id\":\"%s\",\"user_id\":\"%s\",\"user_name\":\"%s\",\"exp\":%d}",
                    roomId, userId, userName, expireTime);

            // AES-128-CBC encryption
            byte[] key = serverSecret.substring(0, 16).getBytes(StandardCharsets.US_ASCII);
            byte[] iv  = buildIv(expireTime, nonce);

            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE,
                    new javax.crypto.spec.SecretKeySpec(key, "AES"),
                    new javax.crypto.spec.IvParameterSpec(iv));
            byte[] encryptedPayload = cipher.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            // Build binary token: version(2) + expire(4) + nonce(8) + payload_len(4) + payload
            ByteBuffer buf = ByteBuffer.allocate(2 + 4 + 8 + 4 + encryptedPayload.length);
            buf.putShort((short) 4);                       // version = 04
            buf.putInt((int) expireTime);
            buf.putLong(nonce);
            buf.putInt(encryptedPayload.length);
            buf.put(encryptedPayload);

            return Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            log.error("ZEGO token generation failed", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private byte[] buildIv(long expireTime, long nonce) {
        ByteBuffer ivBuf = ByteBuffer.allocate(16);
        ivBuf.putInt((int) expireTime);
        ivBuf.putLong(nonce);
        ivBuf.putInt(0); // padding
        return ivBuf.array();
    }
}
