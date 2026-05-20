package com.airbnb.backend.controller;

import com.airbnb.backend.dto.response.PagedResponse;
import com.airbnb.backend.dto.response.PropertyResponse;
import com.airbnb.backend.security.UserPrincipal;
import com.airbnb.backend.service.WishlistService;
import com.airbnb.backend.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/{propertyId}")
    public ResponseEntity<ApiResponse<Void>> toggleWishlist(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long propertyId) {

        wishlistService.toggleWishlist(currentUser.getId(), propertyId);
        return ResponseEntity.ok(
                ApiResponse.success("Wishlist updated"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PropertyResponse>>>
    getWishlist(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PagedResponse<PropertyResponse> response = wishlistService
                .getUserWishlist(currentUser.getId(), page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Wishlist fetched", response));
    }
}