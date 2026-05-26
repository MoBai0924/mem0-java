package com.mem0.server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Rate limiting filter using token bucket algorithm.
 *
 * @author MoBai

 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        // Determine rate limit key
        String key = resolveKey(request);

        if (key != null && !rateLimitService.allowRequest(key)) {
            response.setStatus(429);
            response.setHeader("X-RateLimit-Limit", "100");
            response.setHeader("X-RateLimit-Remaining", "0");
            response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"code\":\"RATE_LIMIT_EXCEEDED\"}");
            return;
        }

        // Add rate limit headers
        if (key != null) {
            long remaining = rateLimitService.getRemainingTokens(key);
            response.setHeader("X-RateLimit-Limit", "100");
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        }

        filterChain.doFilter(request, response);
    }

    private String resolveKey(HttpServletRequest request) {
        // Use user ID if authenticated
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr instanceof UUID userId) {
            return "user:" + userId;
        }

        // Use API key ID if present
        Object apiKeyIdAttr = request.getAttribute("apiKeyId");
        if (apiKeyIdAttr instanceof UUID apiKeyId) {
            return "apikey:" + apiKeyId;
        }

        // Use client IP as fallback
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        return "ip:" + clientIp;
    }
}
