package com.example.demo;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class UserIdCookieFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "cc_user_id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String userId = null;

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (COOKIE_NAME.equals(c.getName())) {
                    userId = c.getValue();
                    break;
                }
            }
        }

        if (userId == null || userId.isBlank()) {
            userId = UUID.randomUUID().toString();

            Cookie cookie = new Cookie(COOKIE_NAME, userId);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(60 * 60 * 24 * 365 * 5); // 5年
            // cookie.setSecure(true); // HTTPSの時だけON
            response.addCookie(cookie);
        }

        // Controller側で使えるように載せる
        request.setAttribute("userId", userId);

        filterChain.doFilter(request, response);
    }
}
