package com.aalap.aalapbackend.dto;

import lombok.Data;

/**
 * Lightweight user snapshot embedded inside thread/contribution responses.
 * Email is intentionally omitted — any authenticated user can read these
 * responses, so exposing emails would be a privacy violation.
 * Use UserProfileResponse (returned by /api/users/me) when the caller's own
 * email is needed.
 */
@Data
public class UserInfo {
    long   id;
    String name;
    String gravatarUrl;
    String profilePicture;
}
