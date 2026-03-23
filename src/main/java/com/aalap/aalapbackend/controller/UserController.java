package com.aalap.aalapbackend.controller;

import com.aalap.aalapbackend.dto.UserProfileResponse;
import com.aalap.aalapbackend.security.JwtUtil;
import com.aalap.aalapbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    UserService userService;
    JwtUtil jwtUtil;

    @Autowired
    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/{userId}")
    public UserProfileResponse getUser(@PathVariable long userId) {
        return userService.getUserProfile(userId);
    }

    // NEW endpoint for viewing MY profile (Your optimized way!)
    @GetMapping("/me")
    public UserProfileResponse getMyProfile(@RequestHeader("Authorization") String authHeader) {
        // 1. Strip the "Bearer " prefix from the header to get the raw token
        String token = authHeader.substring(7);

        // 2. Read the ID directly off the token
        Long userId = jwtUtil.extractUserId(token);

        // 3. Hand it directly to the Sound Engineer! No email lookups required.
        return userService.getUserProfile(userId);
    }
}
