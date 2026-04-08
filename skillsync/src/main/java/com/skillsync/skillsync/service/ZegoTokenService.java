package com.skillsync.skillsync.service;

import com.skillsync.skillsync.exception.AppException;
import com.skillsync.skillsync.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

/**
 * ZEGO Token04 server-side token generation.
 * Follows the official Zego Cloud implementation: AES/CBC/PKCS5Padding
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

    public String generateToken(String roomId, String userId, String userName) {
        if (serverSecret == null || serverSecret.isBlank() || serverSecret.length() != 32) {
            log.error("zego.server-secret must be exactly 32 characters long from console.zegocloud.com");
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        try {
            long nowTime = System.currentTimeMillis() / 1000;
            long expireTime = nowTime + DEFAULT_EXPIRE_SECONDS;
            int nonce = new SecureRandom().nextInt();

            // Payload có thể để trống ("") cho Zego UI Kit Prebuilt
            String payload = "";

            String content = String.format(
                    "{\"app_id\":%d,\"user_id\":\"%s\",\"ctime\":%d,\"expire\":%d,\"nonce\":%d,\"payload\":\"%s\"}",
                    appId, userId, nowTime, expireTime, nonce, payload
            );

            byte[] ivBytes = new byte[16];
            new SecureRandom().nextBytes(ivBytes);

            byte[] contentBytes = encrypt(content.getBytes(StandardCharsets.UTF_8), serverSecret.getBytes(StandardCharsets.UTF_8), ivBytes);

            ByteBuffer buffer = ByteBuffer.allocate(8 + 2 + ivBytes.length + 2 + contentBytes.length);
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.putLong(expireTime);
            buffer.putShort((short) ivBytes.length);
            buffer.put(ivBytes);
            buffer.putShort((short) contentBytes.length);
            buffer.put(contentBytes);

            return "04" + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            log.error("ZEGO token generation failed", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private byte[] encrypt(byte[] content, byte[] secretKey, byte[] ivBytes) throws Exception {
        SecretKeySpec key = new SecretKeySpec(secretKey, "AES");
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        return cipher.doFinal(content);
    }
}