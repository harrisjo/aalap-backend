package com.aalap.aalapbackend.security;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token blacklist.
 *
 * When a user deletes their account, their userId is recorded here with the
 * invalidation timestamp. JwtFilter checks this on every request — any token
 * issued BEFORE the invalidation time is rejected even if it is still
 * cryptographically valid.
 *
 * NOTE: This is an in-memory store that resets on app restart.
 * For production at scale, replace the ConcurrentHashMap with a Redis-backed
 * store (e.g., Spring Data Redis with a TTL equal to jwt.expiration).
 */
@Service
public class TokenBlacklistService {

    // userId → epoch-millis at which all their tokens were invalidated
    private final Map<Long, Long> blacklist = new ConcurrentHashMap<>();

    /** Record the current moment as the invalidation time for the given user. */
    public void invalidate(Long userId) {
        blacklist.put(userId, System.currentTimeMillis());
    }

    /**
     * Returns true if the token (identified by its issuedAt Date) was issued
     * BEFORE the stored invalidation timestamp — i.e., it must be rejected.
     */
    public boolean isInvalidated(Long userId, Date tokenIssuedAt) {
        Long invalidatedAt = blacklist.get(userId);
        return invalidatedAt != null
                && tokenIssuedAt != null
                && tokenIssuedAt.getTime() < invalidatedAt;
    }
}

