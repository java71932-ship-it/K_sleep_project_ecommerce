package com.example.demo;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    public String getTokenFromRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public boolean validateToken(String token) {
        return JwtUtil.validateTokenAndGetEmail(token) != null;
    }

    public String getEmailFromToken(String token) {
        return JwtUtil.validateTokenAndGetEmail(token);
    }
}
