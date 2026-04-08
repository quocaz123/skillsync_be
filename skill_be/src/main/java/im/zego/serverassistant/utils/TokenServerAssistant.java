package im.zego.serverassistant.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Official ZEGOCLOUD Token04 generator — adapted for Spring Boot (Jackson instead of json-simple).
 * Source: https://github.com/zegoim/zego_server_assistant/tree/master/token/java/token04
 *
 * Algorithm: AES-128-GCM, key = secret.getBytes("UTF-8"), Big-Endian layout.
 */
public class TokenServerAssistant {

    private static final String VERSION_FLAG = "04";
    private static final int NONCE_LENGTH = 12;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static class ErrorCode {
        public static final int SUCCESS = 0;
        public static final int ILLEGAL_APP_ID = 1;
        public static final int ILLEGAL_USER_ID = 3;
        public static final int ILLEGAL_SECRET = 5;
        public static final int ILLEGAL_EFFECTIVE_TIME = 6;
        public static final int OTHER = -1;
    }

    public static class ErrorInfo {
        public int code;
        public String message;

        ErrorInfo() {
            code = ErrorCode.SUCCESS;
            message = "";
        }

        @Override
        public String toString() {
            return "{\"code\": " + code + ", \"message\": \"" + message + "\"}";
        }
    }

    public static class TokenInfo {
        public String data = "";
        public ErrorInfo error;

        TokenInfo() {
            this.error = new ErrorInfo();
        }

        @Override
        public String toString() {
            return "TokenInfo {\"error\": " + error + ", \"data\": \"" + data + "\"}";
        }
    }

    private TokenServerAssistant() {}

    /**
     * Generate ZEGOCLOUD Token04.
     *
     * @param appId                  ZEGOCLOUD AppID
     * @param userId                 User ID (max 64 chars)
     * @param secret                 Server Secret (exactly 32 chars from console.zegocloud.com)
     * @param effectiveTimeInSeconds Token validity (seconds)
     * @param payload                Optional extra payload string (pass "" for basic auth)
     * @return TokenInfo — check error.code == SUCCESS before using token.data
     */
    public static TokenInfo generateToken04(long appId, String userId, String secret,
                                            int effectiveTimeInSeconds, String payload) {
        TokenInfo token = new TokenInfo();

        if (appId == 0) {
            token.error.code = ErrorCode.ILLEGAL_APP_ID;
            token.error.message = "illegal appId";
            return token;
        }

        if (userId == null || userId.isEmpty() || userId.length() > 64) {
            token.error.code = ErrorCode.ILLEGAL_USER_ID;
            token.error.message = "userId can't be empty and must not exceed 64 chars";
            return token;
        }

        if (secret == null || secret.length() != 32) {
            token.error.code = ErrorCode.ILLEGAL_SECRET;
            token.error.message = "secret must be exactly 32 characters";
            return token;
        }

        if (effectiveTimeInSeconds <= 0) {
            token.error.code = ErrorCode.ILLEGAL_EFFECTIVE_TIME;
            token.error.message = "effectiveTimeInSeconds must be > 0";
            return token;
        }

        try {
            // Random 12-byte nonce for AES-GCM
            byte[] nonceBytes = new byte[NONCE_LENGTH];
            new SecureRandom().nextBytes(nonceBytes);

            long nowTime = System.currentTimeMillis() / 1000;
            long expireTime = nowTime + effectiveTimeInSeconds;
            int nonce = new Random().nextInt();

            // Build JSON payload (matches official format exactly)
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("app_id", appId);
            json.put("user_id", userId);
            json.put("nonce", nonce);
            json.put("ctime", nowTime);
            json.put("expire", expireTime);
            json.put("payload", payload != null ? payload : "");

            byte[] contentBytes = encrypt(MAPPER.writeValueAsBytes(json), secret, nonceBytes);

            // Binary layout (BIG_ENDIAN):
            //   8 bytes expire_time (long)
            //   2 bytes nonce_len + 12 bytes nonce
            //   2 bytes content_len + N bytes encrypted content
            //   1 byte mode = 1 (GCM)
            ByteBuffer buffer = ByteBuffer.wrap(new byte[contentBytes.length + NONCE_LENGTH + 13]);
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.putLong(expireTime);
            packBytes(nonceBytes, buffer);
            packBytes(contentBytes, buffer);
            buffer.put((byte) 1); // AesEncryptModeGCM

            token.data = VERSION_FLAG + Base64.getEncoder().encodeToString(buffer.array());
            token.error.code = ErrorCode.SUCCESS;

        } catch (Exception e) {
            token.error.code = ErrorCode.OTHER;
            token.error.message = e.getMessage();
        }

        return token;
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private static byte[] encrypt(byte[] content, String secretKey, byte[] nonce) throws Exception {
        SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
        return cipher.doFinal(content);
    }

    private static void packBytes(byte[] src, ByteBuffer target) {
        target.putShort((short) src.length);
        target.put(src);
    }
}
