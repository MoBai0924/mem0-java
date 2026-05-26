package com.mem0.server.security;

import com.mem0.server.domain.model.RequestLog;
import com.mem0.server.repository.RequestLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that logs all API requests to the database.
 *
 * @author MoBai

 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private final RequestLogRepository requestLogRepository;

    public RequestLoggingFilter(RequestLogRepository requestLogRepository) {
        this.requestLogRepository = requestLogRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequest(request, response, duration);
        }
    }

    @Async
    protected void logRequest(HttpServletRequest request, HttpServletResponse response, long duration) {
        try {
            String method = request.getMethod();
            String path = request.getRequestURI();

            // Skip actuator and swagger requests
            if (path.startsWith("/actuator") || path.startsWith("/swagger") || path.startsWith("/api-docs")) {
                return;
            }

            RequestLog requestLog = new RequestLog(method, path, response.getStatus(), duration);

            // Client IP
            String clientIp = request.getHeader("X-Forwarded-For");
            if (clientIp == null || clientIp.isEmpty()) {
                clientIp = request.getRemoteAddr();
            }
            //requestLog.setClientIp(clientIp);

            if (requestLog.getId() == null) {
                requestLog.setId(UUID.randomUUID());
            }
            requestLog.setAuthType("none");
            requestLogRepository.save(requestLog);
        } catch (Exception e) {
            log.debug("Failed to log request: {}", e.getMessage());
        }
    }
}
