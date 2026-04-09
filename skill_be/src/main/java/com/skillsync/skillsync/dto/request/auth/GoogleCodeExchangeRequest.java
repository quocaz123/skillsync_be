package com.skillsync.skillsync.dto.request.auth;

import lombok.Data;

@Data
public class GoogleCodeExchangeRequest {
    private String code;
    private String redirectUri;
}

