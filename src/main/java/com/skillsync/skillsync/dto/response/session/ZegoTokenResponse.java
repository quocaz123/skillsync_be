package com.skillsync.skillsync.dto.response.session;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ZegoTokenResponse {
    private String token;
    private Long appId;
    private String roomId;
    private String userId;
    private String userName;
}
