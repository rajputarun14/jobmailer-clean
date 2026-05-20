package com.arun.jobmailer.rateLimiter.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.arun.jobmailer.rateLimiter.service.RateLimiterRedisService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter{
    private final RateLimiterRedisService rateLimiterService;

    public RateLimitFilter(RateLimiterRedisService rateLimiterService){
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("GET".equalsIgnoreCase(request.getMethod())) return true;
        return !(path.equals("/send")
                || path.startsWith("/ai/")
                || path.equals("/uploadResume")
                || path.startsWith("/emails/")
                || path.startsWith("/admin/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String userId = getUserId(request);
        boolean allowed = rateLimiterService.allowRequest(userId, 10, 1);
        if(!allowed){
            response.setStatus(429);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Retry-After", "1");
            response.getWriter().write("{\"code\":\"RATE_LIMIT_EXCEEDED\",\"error\":\"Too many requests. Please wait and try again.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String getUserId(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return request.getRemoteAddr();
    }
}
