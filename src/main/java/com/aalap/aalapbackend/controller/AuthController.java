package com.aalap.aalapbackend.controller;

import com.aalap.aalapbackend.dto.AuthResponse;
import com.aalap.aalapbackend.dto.LoginRequest;
import com.aalap.aalapbackend.dto.RegisterRequest;
import com.aalap.aalapbackend.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    AuthService authService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerUser(@Valid @RequestBody RegisterRequest registerRequest,
                                     HttpServletResponse httpResponse) {
        AuthResponse authResponse = authService.register(registerRequest);
        setJwtCookie(httpResponse, authResponse.getToken());
        return authResponse; // token field is @JsonIgnore'd — only name/email/userId sent
    }

    @PostMapping("/login")
    public AuthResponse loginUser(@Valid @RequestBody LoginRequest loginRequest,
                                  HttpServletResponse httpResponse) {
        AuthResponse authResponse = authService.login(loginRequest);
        setJwtCookie(httpResponse, authResponse.getToken());
        return authResponse;
    }

    // ─── LOGOUT ──────────────────────────────────────────────────────────────
    // Clears the HttpOnly JWT cookie server-side. The browser has no JS access
    // to the cookie so the client can't do this itself.

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse httpResponse) {
        clearJwtCookie(httpResponse);
        return ResponseEntity.noContent().build(); // 204
    }

    // ─── COOKIE HELPERS ──────────────────────────────────────────────────────

    private void setJwtCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);   // JS cannot read this cookie — XSS-proof
        cookie.setSecure(true);     // Only sent over HTTPS
        cookie.setPath("/");
        cookie.setMaxAge((int)(jwtExpiration / 1000)); // convert ms → seconds
        cookie.setAttribute("SameSite", "None"); // Required for cross-origin (Vercel → HF Space)
        response.addCookie(cookie);
    }

    private void clearJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Immediately expire
        cookie.setAttribute("SameSite", "None");
        response.addCookie(cookie);
    }
}
