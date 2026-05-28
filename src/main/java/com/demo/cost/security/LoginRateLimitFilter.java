package com.demo.cost.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP rate limit for /api/auth/login and /api/auth/register.
 * 5 attempts per minute. In-memory Bucket4j — fine for single-node deploys;
 * swap to a distributed backing (Hazelcast/Redis) when scaling out.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket bucketFor(String ip) {
        return buckets.computeIfAbsent(ip, k -> Bucket.builder()
                .addLimit(Bandwidth.simple(5, Duration.ofMinutes(1)))
                .build());
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // First entry is the original client per the proxy chain
            int comma = xff.indexOf(',');
            return (comma < 0 ? xff : xff.substring(0, comma)).trim();
        }
        return req.getRemoteAddr();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getRequestURI();
        if ("POST".equalsIgnoreCase(req.getMethod()) &&
            (path.equals("/api/auth/login") || path.equals("/api/auth/register"))) {
            if (!bucketFor(clientIp(req)).tryConsume(1)) {
                res.setStatus(429);
                res.setHeader("Retry-After", "60");
                res.setContentType("application/json");
                res.getWriter().write("{\"error\":\"too_many_requests\",\"message\":\"Too many login attempts. Try again in a minute.\"}");
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
