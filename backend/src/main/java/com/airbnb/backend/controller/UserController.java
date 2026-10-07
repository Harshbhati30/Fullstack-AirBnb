package com.airbnb.backend.controller;

import com.airbnb.backend.dto.request.ChangePasswordRequest;
import com.airbnb.backend.dto.request.UpdateProfileRequest;
import com.airbnb.backend.dto.response.AuthResponse;
import com.airbnb.backend.dto.response.PublicUserResponse;
import com.airbnb.backend.dto.response.UserResponse;
import com.airbnb.backend.security.UserPrincipal;
import com.airbnb.backend.service.AuthService;
import com.airbnb.backend.service.UserService;
import com.airbnb.backend.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                "User profile fetched", userService.getUserById(currentUser.getId())));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PublicUserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "User fetched", userService.getPublicUserById(id)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully",
                userService.updateProfile(currentUser.getId(), request)));
    }

    @PostMapping("/me/image")
    public ResponseEntity<ApiResponse<UserResponse>> uploadProfileImage(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Profile image uploaded",
                userService.uploadProfileImage(currentUser.getId(), file)));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }


    @PostMapping("/become-host")
    public ResponseEntity<ApiResponse<AuthResponse>> becomeHost(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ApiResponse.success("You are now a host",
                authService.becomeHost(currentUser.getId())));
    }
}