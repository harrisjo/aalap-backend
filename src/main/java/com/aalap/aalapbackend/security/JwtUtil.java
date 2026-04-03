package com.aalap.aalapbackend.security;

import com.aalap.aalapbackend.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // ─── STARTUP VALIDATION ──────────────────────────────────────────────────
    // Fail fast if JWT_SECRET is too short. HMAC-SHA256 requires at least
    // 256 bits (32 characters) for the key to be considered secure.
    @PostConstruct
    public void validateSecret() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                "JWT_SECRET must be at least 32 characters (256 bits). " +
                "Current length: " + (secret != null ? secret.length() : 0)
            );
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("name", user.getName())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public Long extractUserId(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userId", Long.class);
    }

    /** Used by JwtFilter to check the token against the blacklist. */
    public Date extractIssuedAt(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getIssuedAt();
    }

    public boolean validateToken(String token) {
        try {
            return extractEmail(token) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
