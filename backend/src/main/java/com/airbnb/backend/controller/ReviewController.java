package com.airbnb.backend.controller;

import com.airbnb.backend.dto.request.ReviewRequest;
import com.airbnb.backend.dto.response.PagedResponse;
import com.airbnb.backend.dto.response.ReviewResponse;
import com.airbnb.backend.security.UserPrincipal;
import com.airbnb.backend.service.ReviewService;
import com.airbnb.backend.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody ReviewRequest request) {

        ReviewResponse response = reviewService
                .createReview(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted", response));
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>>
    getPropertyReviews(
            @PathVariable Long propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PagedResponse<ReviewResponse> response = reviewService
                .getPropertyReviews(propertyId, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Reviews fetched", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id) {

        reviewService.deleteReview(currentUser.getId(), id);
        return ResponseEntity.ok(
                ApiResponse.success("Review deleted"));
    }
}