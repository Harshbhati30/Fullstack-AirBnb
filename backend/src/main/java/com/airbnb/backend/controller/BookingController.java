package com.airbnb.backend.controller;

import com.airbnb.backend.dto.request.BookingRequest;
import com.airbnb.backend.dto.response.BookingResponse;
import com.airbnb.backend.dto.response.PagedResponse;
import com.airbnb.backend.security.UserPrincipal;
import com.airbnb.backend.service.BookingService;
import com.airbnb.backend.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody BookingRequest request) {

        BookingResponse response = bookingService
                .createBooking(currentUser.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id) {

        BookingResponse response = bookingService
                .getBookingById(currentUser.getId(), id);
        return ResponseEntity.ok(
                ApiResponse.success("Booking fetched", response));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>>
    getMyBookings(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PagedResponse<BookingResponse> response = bookingService
                .getUserBookings(currentUser.getId(), page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Bookings fetched", response));
    }

    @GetMapping("/host/bookings")
    @PreAuthorize("hasAnyAuthority('ROLE_HOST','ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>>
    getHostBookings(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PagedResponse<BookingResponse> response = bookingService
                .getHostBookings(currentUser.getId(), page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Host bookings fetched", response));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {

        BookingResponse response = bookingService
                .cancelBooking(currentUser.getId(), id, reason);
        return ResponseEntity.ok(
                ApiResponse.success("Booking cancelled", response));
    }
}