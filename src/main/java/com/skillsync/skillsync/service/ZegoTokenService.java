package com.skillsync.skillsync.service;

import com.skillsync.skillsync.exception.AppException;
import com.skillsync.skillsync.exception.ErrorCode;
import im.zego.serverassistant.utils.TokenServerAssistant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * ZEGOCLOUD Token04 service using the official TokenServerAssistant generator.
 * Algorithm: AES-128-GCM, Big-Endian layout, key = serverSecret.getBytes("UTF-8").
 * ServerSecret must be exactly 32 chars from console.zegocloud.com.
 */
@Service
@Slf4j
public class ZegoTokenService {

    @Value("${zego.app-id}")
    private Long appId;

    @Value("${zego.server-secret}")
    private String serverSecret;

    public Long getAppId() {
        return appId;
    }

    /**
     * Generate a ZEGOCLOUD Token04 for the given user/room.
     * Called by SessionService.getZegoToken().
     *
     * @param roomId   session.videoRoomId
     * @param userId   user UUID string
     * @param userName user display name
     * @param expireSeconds duration in seconds for token validity
     * @return Token04 string starting with "04..."
     */
    public String generateToken(String roomId, String userId, String userName, int expireSeconds) {
        if (serverSecret == null || serverSecret.length() != 32) {
            log.error("ZEGO_SERVER_SECRET must be exactly 32 chars. Current length: {}",
                    serverSecret == null ? "null" : serverSecret.length());
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        TokenServerAssistant.TokenInfo tokenInfo = TokenServerAssistant.generateToken04(
                appId,
                userId,
                serverSecret,
                expireSeconds,
                "" // empty payload — room access controlled by session-level auth in SessionService
        );

        if (tokenInfo.error.code != TokenServerAssistant.ErrorCode.SUCCESS) {
            log.error("ZEGO token generation failed: {}", tokenInfo.error);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        log.debug("ZEGO token generated for userId={} roomId={} length={}",
                userId, roomId, tokenInfo.data.length());
        return tokenInfo.data;
    }
}