package com.aalap.aalapbackend.service;

import com.aalap.aalapbackend.dto.AuthResponse;
import com.aalap.aalapbackend.dto.LoginRequest;
import com.aalap.aalapbackend.dto.RegisterRequest;
import com.aalap.aalapbackend.entity.User;
import com.aalap.aalapbackend.exception.EmailAlreadyExistsException;
import com.aalap.aalapbackend.exception.NullUserException;
import com.aalap.aalapbackend.exception.WrongPasswordException;
import com.aalap.aalapbackend.repository.UserRepository;
import com.aalap.aalapbackend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    JwtUtil jwtUtil;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
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

        String token = jwtUtil.generateToken(newUser);
        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken(token);
        return authResponse;
    }

    public AuthResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail()).orElse(null);
        if(user == null){
            throw new NullUserException("Invalid email - User not found with this email!");
        } else if(!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())){
            throw new WrongPasswordException("Invalid password - Try again!");
        }
        String token = jwtUtil.generateToken(user);
        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken(token);
        return authResponse;
    }
}
