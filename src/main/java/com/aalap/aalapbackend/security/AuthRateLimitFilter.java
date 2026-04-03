package com.aalap.aalapbackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP-based sliding-window rate limiter for /api/auth/ endpoints.
 *
 * Allows MAX_ATTEMPTS requests per IP within WINDOW_MILLIS.
 * Excess requests receive a 429 Too Many Requests response.
 * No external dependencies required — uses a ConcurrentHashMap per JVM instance.
 *
 * NOTE: For multi-instance deployments, replace with a Redis-backed counter.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final int  MAX_ATTEMPTS  = 10;           // requests
    private static final long WINDOW_MILLIS = 60_000L;      // per 60 seconds

    // IP address → ring buffer of request timestamps within the current window
    private final Map<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only rate-limit auth endpoints; all other paths pass straight through.
        return !request.getRequestURI().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ip  = getClientIp(request);
        long   now = System.currentTimeMillis();

        // Atomically append the current timestamp and evict stale ones
        attempts.compute(ip, (key, deque) -> {
            if (deque == null) deque = new ArrayDeque<>();
            while (!deque.isEmpty() && now - deque.peekFirst() > WINDOW_MILLIS) {
                deque.pollFirst();
            }
            deque.addLast(now);
            return deque;
        });

        Deque<Long> window = attempts.get(ip);
        if (window != null && window.size() > MAX_ATTEMPTS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Too many requests. Please wait a minute before trying again.\"}"
            );
            return; // short-circuit — do not continue the filter chain
        }

        filterChain.doFilter(request, response);
    }

    /** Returns the real client IP, respecting X-Forwarded-For from a reverse proxy. */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim(); // first hop is the real client
        }
        return request.getRemoteAddr();
    }
}

