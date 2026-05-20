package com.airbnb.backend.controller;

import com.airbnb.backend.dto.request.UpdateProfileRequest;
import com.airbnb.backend.dto.response.UserResponse;
import com.airbnb.backend.security.UserPrincipal;
import com.airbnb.backend.service.UserService;
import com.airbnb.backend.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        UserResponse response = userService.getUserById(currentUser.getId());
        return ResponseEntity.ok(
                ApiResponse.success("User profile fetched", response));
    }


    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable Long id) {

        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(
                ApiResponse.success("User fetched", response));
    }


    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {

        UserResponse response = userService.updateProfile(
                currentUser.getId(), request);
        return ResponseEntity.ok(
                ApiResponse.success("Profile updated successfully", response));
    }


    @PostMapping("/me/image")
    public ResponseEntity<ApiResponse<UserResponse>> uploadProfileImage(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam("file") MultipartFile file) {

        UserResponse response = userService.uploadProfileImage(
                currentUser.getId(), file);
        return ResponseEntity.ok(
                ApiResponse.success("Profile image uploaded", response));
    }
}