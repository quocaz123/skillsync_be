package com.skillsync.skillsync.service;

import org.springframework.stereotype.Service;
import com.skillsync.skillsync.configuration.CookieProperties;
import com.skillsync.skillsync.constant.CookieNames;
import com.skillsync.skillsync.dto.response.auth.AuthenticationResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CookieService {
    
    private final CookieProperties cookieProperties;
    
    public void setAuthCookies(HttpServletResponse response, AuthenticationResponse auth) {
        // Match JwtService access token expiry (1 hour)
        response.addCookie(buildCookie(CookieNames.ACCESS_TOKEN, auth.getAccessToken(), 60 * 60));
        response.addCookie(buildCookie(CookieNames.REFRESH_TOKEN, auth.getRefreshToken(), 60 * 60 * 24 * 7));
    }

    public void clearAuthCookies(HttpServletResponse response) {
        response.addCookie(buildCookie(CookieNames.ACCESS_TOKEN, null, 0));
        response.addCookie(buildCookie(CookieNames.REFRESH_TOKEN, null, 0));
    }

    private Cookie buildCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setSecure(cookieProperties.isSecure());
        cookie.setAttribute("SameSite", cookieProperties.getSameSite());
        return cookie;
    }
}
