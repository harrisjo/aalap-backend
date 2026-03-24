package com.aalap.aalapbackend.controller;

import com.aalap.aalapbackend.dto.AuthResponse;
import com.aalap.aalapbackend.dto.LoginRequest;
import com.aalap.aalapbackend.dto.RegisterRequest;
import com.aalap.aalapbackend.entity.User;
import com.aalap.aalapbackend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
         return authService.register(registerRequest);
    }

    @PostMapping("/login")
    public AuthResponse loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest);
    }
}
