package com.aalap.aalapbackend.service;

import com.aalap.aalapbackend.dto.AuthResponse;
import com.aalap.aalapbackend.dto.LoginRequest;
import com.aalap.aalapbackend.dto.RegisterRequest;
import com.aalap.aalapbackend.entity.User;
import com.aalap.aalapbackend.exception.EmailAlreadyExistsException;
import com.aalap.aalapbackend.repository.UserRepository;
import com.aalap.aalapbackend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    JwtUtil jwtUtil;
    EmailService emailService;
///test
    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        User user = userRepository.findByEmail(registerRequest.getEmail()).orElse(null);
        if(user != null){
            throw new EmailAlreadyExistsException("Email already exists");
        }

        String hashedPassword = passwordEncoder.encode(registerRequest.getPassword());
        User newUser = new User();
        newUser.setName(registerRequest.getName());
        newUser.setEmail(registerRequest.getEmail());
        newUser.setPassword(hashedPassword);
        userRepository.save(newUser);

        // Fire-and-forget welcome email — never blocks registration
        emailService.sendWelcomeEmail(newUser.getEmail(), newUser.getName());

        // Token goes into the HttpOnly cookie (set by AuthController).
        // Response body carries only non-sensitive user info.
        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken(jwtUtil.generateToken(newUser));
        authResponse.setUserId(newUser.getId());
        authResponse.setName(newUser.getName());
        authResponse.setEmail(newUser.getEmail());
        return authResponse;
    }

    public AuthResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail()).orElse(null);
        // Single generic message for "no such user", "wrong password", AND "soft-deleted account" —
        // all three cases return the same 401 to prevent email-enumeration attacks.
        if (user == null || !user.isEnabled() || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken(jwtUtil.generateToken(user));
        authResponse.setUserId(user.getId());
        authResponse.setName(user.getName());
        authResponse.setEmail(user.getEmail());
        return authResponse;
    }
}
