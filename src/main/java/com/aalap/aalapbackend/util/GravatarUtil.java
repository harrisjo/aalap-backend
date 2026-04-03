package com.aalap.aalapbackend.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Computes Gravatar avatar URLs from an email address.
 * Uses MD5 hashing (as required by the Gravatar API) — this is not a security operation,
 * it is only used to derive a publicly-queryable avatar URL.
 * Defaults to "identicon" so every user gets a unique generated avatar if they
 * haven't uploaded a Gravatar.
 */
public final class GravatarUtil {

    private GravatarUtil() {}

    public static String getUrl(String email) {
        if (email == null || email.isBlank()) {
            return "https://www.gravatar.com/avatar/?s=200&d=identicon";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(
                    email.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(32);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return "https://www.gravatar.com/avatar/" + hex + "?s=200&d=identicon";
        } catch (NoSuchAlgorithmException e) {
            // MD5 is guaranteed to be present in every JVM; this is unreachable.
            return "https://www.gravatar.com/avatar/?s=200&d=identicon";
        }
    }
}

