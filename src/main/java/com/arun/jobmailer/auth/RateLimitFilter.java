package com.arun.jobmailer.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxRequests;
    private final long windowMillis;

    @Autowired
    public RateLimitFilter(@Value("${security.rate-limit.max-requests:30}") int maxRequests,
                           @Value("${security.rate-limit.window-seconds:60}") long windowSeconds) {
        this(maxRequests, windowSeconds, Clock.systemUTC());
    }

    RateLimitFilter(int maxRequests, long windowSeconds, Clock clock) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000;
        this.clock = clock;
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
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = request.getUserPrincipal() == null
                ? request.getRemoteAddr()
                : request.getUserPrincipal().getName();
        if (!allow(key)) {
            int retryAfterSecs = Math.max(1, (int) (windowMillis / 1000));
            String message = String.format(Locale.US,
                    "Too many requests or uploads in a short time. Please wait about %d seconds, then try again.",
                    retryAfterSecs);
            String body = String.format(Locale.US,
                    "{\"code\":\"RATE_LIMIT_EXCEEDED\",\"error\":%s,\"retryAfterSeconds\":%d}",
                    jsonString(message),
                    retryAfterSecs);
            response.setStatus(429);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Retry-After", String.valueOf(retryAfterSecs));
            response.getWriter().write(body);
            return;
        }
        filterChain.doFilter(request, response);
    }

    boolean allow(String key) {
        long now = clock.millis();
        Bucket bucket = buckets.compute(key, (ignored, existing) -> {
            if (existing == null || now - existing.windowStart >= windowMillis) {
                return new Bucket(now, 1);
            }
            existing.count++;
            return existing;
        });
        return bucket.count <= maxRequests;
    }

    private static String jsonString(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format(Locale.US, "\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static final class Bucket {
        private final long windowStart;
        private int count;

        private Bucket(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
