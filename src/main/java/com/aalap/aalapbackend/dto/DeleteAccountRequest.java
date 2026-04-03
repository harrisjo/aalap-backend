package com.aalap.aalapbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for DELETE /api/users/me.
 * Re-authentication with the account password is required before deletion
 * to prevent an attacker with a stolen session from wiping the account.
 */
@Data
public class DeleteAccountRequest {

    @NotBlank(message = "Password is required to confirm account deletion")
    private String password;
}

