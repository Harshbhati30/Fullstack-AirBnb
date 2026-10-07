package com.airbnb.backend.controller;

import com.airbnb.backend.dto.request.PropertyRequest;
import com.airbnb.backend.dto.request.PropertySearchRequest;
import com.airbnb.backend.dto.response.PagedResponse;
import com.airbnb.backend.dto.response.PropertyImageResponse;
import com.airbnb.backend.dto.response.PropertyResponse;
import com.airbnb.backend.security.UserPrincipal;
import com.airbnb.backend.service.PropertyService;
import com.airbnb.backend.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
public class PropertyController {

    private static final String ADMIN = "ROLE_ADMIN";

    private final PropertyService propertyService;


    @GetMapping({"", "/search"})
    public ResponseEntity<ApiResponse<PagedResponse<PropertyResponse>>> searchProperties(
            PropertySearchRequest filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PagedResponse<PropertyResponse> response =
                propertyService.searchProperties(filters, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Properties fetched", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PropertyResponse>> getPropertyById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Property fetched", propertyService.getPropertyById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_HOST','ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PropertyResponse>> createProperty(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody PropertyRequest request) {

        PropertyResponse response = propertyService.createProperty(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Property created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_HOST','ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PropertyResponse>> updateProperty(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id,
            @Valid @RequestBody PropertyRequest request) {

        PropertyResponse response = propertyService.updateProperty(
                currentUser.getId(), currentUser.hasRole(ADMIN), id, request);
        return ResponseEntity.ok(ApiResponse.success("Property updated successfully", response));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_HOST','ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PropertyResponse>> setStatus(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id,
            @RequestParam boolean active) {

        PropertyResponse response = propertyService.setActive(
                currentUser.getId(), currentUser.hasRole(ADMIN), id, active);
        return ResponseEntity.ok(ApiResponse.success(
                active ? "Property activated" : "Property deactivated", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_HOST','ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProperty(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id) {

        propertyService.deleteProperty(currentUser.getId(), currentUser.hasRole(ADMIN), id);
        return ResponseEntity.ok(ApiResponse.success("Property deleted successfully"));
    }

    @PostMapping("/{id}/images")
    @PreAuthorize("hasAnyAuthority('ROLE_HOST','ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<PropertyImageResponse>>> uploadImages(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files) {

        List<PropertyImageResponse> response = propertyService.uploadPropertyImages(
                currentUser.getId(), currentUser.hasRole(ADMIN), id, files);
        return ResponseEntity.ok(ApiResponse.success("Images uploaded successfully", response));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasAnyAuthority('ROLE_HOST','ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id,
            @PathVariable Long imageId) {

        propertyService.deleteImage(currentUser.getId(), currentUser.hasRole(ADMIN), id, imageId);
        return ResponseEntity.ok(ApiResponse.success("Image deleted"));
    }

    @PutMapping("/{id}/images/{imageId}/primary")
    @PreAuthorize("hasAnyAuthority('ROLE_HOST','ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<PropertyImageResponse>>> setPrimaryImage(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id,
            @PathVariable Long imageId) {

        List<PropertyImageResponse> response = propertyService.setPrimaryImage(
                currentUser.getId(), currentUser.hasRole(ADMIN), id, imageId);
        return ResponseEntity.ok(ApiResponse.success("Primary image updated", response));
    }

    @GetMapping("/host/my-listings")
    @PreAuthorize("hasAnyAuthority('ROLE_HOST','ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<PropertyResponse>>> getMyListings(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PagedResponse<PropertyResponse> response =
                propertyService.getHostProperties(currentUser.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success("Your listings fetched", response));
    }
}