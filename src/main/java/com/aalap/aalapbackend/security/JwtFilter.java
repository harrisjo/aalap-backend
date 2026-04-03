package com.aalap.aalapbackend.security;

import com.aalap.aalapbackend.entity.User;
import com.aalap.aalapbackend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtFilter(UserRepository userRepository,
                     JwtUtil jwtUtil,
                     TokenBlacklistService tokenBlacklistService) {
        this.userRepository          = userRepository;
        this.jwtUtil                 = jwtUtil;
        this.tokenBlacklistService   = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // ── 1. Try HttpOnly cookie first (secure default for browser clients) ──
        String token = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // ── 2. Fall back to Authorization: Bearer header (API clients / Postman) ──
        if (token == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtUtil.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── 3. Reject soft-deleted accounts (persistent DB check) ─────────────
        // isEnabled() returns false once an account is anonymized.
        // This persists across server restarts, unlike the in-memory blacklist.
        if (!user.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── 4. Reject tokens that were invalidated (e.g. after account deletion) ──
        Date issuedAt = jwtUtil.extractIssuedAt(token);
        if (tokenBlacklistService.isInvalidated(user.getId(), issuedAt)) {
            filterChain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        filterChain.doFilter(request, response);
    }
}