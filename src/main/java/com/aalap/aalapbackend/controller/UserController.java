package com.aalap.aalapbackend.controller;

import com.aalap.aalapbackend.dto.DeleteAccountRequest;
import com.aalap.aalapbackend.dto.UserProfileResponse;
import com.aalap.aalapbackend.entity.User;
import com.aalap.aalapbackend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/users")
public class UserController {
    UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    public UserProfileResponse getUser(@PathVariable long userId) {
        return userService.getUserProfile(userId);
    }

    @GetMapping("/me")
    public UserProfileResponse getMyProfile(@AuthenticationPrincipal User user) {
        // JwtFilter already validated the token and stored the User in the SecurityContext —
        // no need to re-parse the JWT here.
        return userService.getUserProfile(user.getId());
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> leaveAalap(
            @RequestBody @Valid DeleteAccountRequest request) throws IOException {
        // Password is verified inside the service before any data is deleted.
        userService.deleteUserAccount(request.getPassword());
        return ResponseEntity.noContent().build();
    }
}
