package com.aalap.aalapbackend.controller;

import com.aalap.aalapbackend.dto.DeleteAccountRequest;
import com.aalap.aalapbackend.dto.UserProfileResponse;
import com.aalap.aalapbackend.entity.User;
import com.aalap.aalapbackend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
        return userService.getUserProfile(user.getId());
    }

    @PatchMapping(value = "/me/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfileResponse uploadProfilePicture(
            @RequestParam("file") MultipartFile file) throws IOException {
        return userService.updateProfilePicture(file);
    }

    @DeleteMapping("/me/picture")
    public ResponseEntity<Void> removeProfilePicture() {
        userService.removeProfilePicture();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> leaveAalap(
            @RequestBody @Valid DeleteAccountRequest request) {
        userService.deleteUserAccount(request.getPassword());
        return ResponseEntity.noContent().build();
    }
}
