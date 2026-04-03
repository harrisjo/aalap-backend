package com.aalap.aalapbackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class AuthResponse {
    // ── HttpOnly cookie migration ─────────────────────────────────────────────
    // The raw JWT is stored in an HttpOnly cookie by AuthController — it must
    // NEVER appear in the JSON response body (XSS would read it from there).
    // @JsonIgnore tells Jackson to skip this field during serialisation.
    @JsonIgnore
    String token;

    // These are the only fields the client receives in the response body.
    Long   userId;
    String name;
    String email;
}
