package com.airbnb.backend.controller;

import com.airbnb.backend.dto.request.AmenityRequest;
import com.airbnb.backend.dto.response.*;
import com.airbnb.backend.security.UserPrincipal;
import com.airbnb.backend.service.AdminService;
import com.airbnb.backend.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("Stats fetched", adminService.getStats()));
    }


    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Users fetched",
                adminService.getUsers(page, size)));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> setUserStatus(
            @AuthenticationPrincipal UserPrincipal admin,
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.success(
                active ? "User activated" : "User deactivated",
                adminService.setUserActive(admin.getId(), id, active)));
    }



    @GetMapping("/properties")
    public ResponseEntity<ApiResponse<PagedResponse<PropertyResponse>>> getProperties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Properties fetched",
                adminService.getProperties(page, size)));
    }

    @PutMapping("/properties/{id}/status")
    public ResponseEntity<ApiResponse<PropertyResponse>> setPropertyStatus(
            @AuthenticationPrincipal UserPrincipal admin,
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.success(
                active ? "Property activated" : "Property deactivated",
                adminService.setPropertyActive(admin.getId(), id, active)));
    }



    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> getBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Bookings fetched",
                adminService.getBookings(page, size)));
    }



    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long id) {
        adminService.deleteReview(id);
        return ResponseEntity.ok(ApiResponse.success("Review deleted"));
    }

    @PostMapping("/amenities")
    public ResponseEntity<ApiResponse<AmenityResponse>> createAmenity(
            @Valid @RequestBody AmenityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Amenity created", adminService.createAmenity(request)));
    }
}