package com.airbnb.backend.controller;

import com.airbnb.backend.dto.request.PropertyRequest;
import com.airbnb.backend.dto.response.PagedResponse;
import com.airbnb.backend.dto.response.PropertyImageResponse;
import com.airbnb.backend.dto.response.PropertyResponse;
import com.airbnb.backend.security.UserPrincipal;
import com.airbnb.backend.service.PropertyService;
import com.airbnb.backend.util.ApiResponse;
import com.airbnb.backend.util.AppConstants;
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

    private final PropertyService propertyService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PropertyResponse>>>
    getAllProperties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PagedResponse<PropertyResponse> response =
                propertyService.getAllProperties(page, size, sortBy, sortDir);
        return ResponseEntity.ok(
                ApiResponse.success("Properties fetched", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PropertyResponse>> getPropertyById(
            @PathVariable Long id) {

        PropertyResponse response = propertyService.getPropertyById(id);
        return ResponseEntity.ok(
                ApiResponse.success("Property fetched", response));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_HOST','ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PropertyResponse>> createProperty(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody PropertyRequest request) {

        PropertyResponse response =
                propertyService.createProperty(currentUser.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Property created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_HOST','ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PropertyResponse>> updateProperty(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id,
            @Valid @RequestBody PropertyRequest request) {

        PropertyResponse response =
                propertyService.updateProperty(currentUser.getId(), id, request);
        return ResponseEntity.ok(
                ApiResponse.success("Property updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_HOST','ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProperty(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id) {

        propertyService.deleteProperty(currentUser.getId(), id);
        return ResponseEntity.ok(
                ApiResponse.success("Property deleted successfully"));
    }

    @PostMapping("/{id}/images")
    @PreAuthorize("hasAnyAuthority('ROLE_HOST','ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<PropertyImageResponse>>>
    uploadImages(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files) {

        List<PropertyImageResponse> response =
                propertyService.uploadPropertyImages(
                        currentUser.getId(), id, files);
        return ResponseEntity.ok(
                ApiResponse.success("Images uploaded successfully", response));
    }


    @GetMapping("/host/my-listings")
    @PreAuthorize("hasAnyAuthority('ROLE_HOST','ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<PropertyResponse>>>
    getMyListings(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PagedResponse<PropertyResponse> response =
                propertyService.getHostProperties(
                        currentUser.getId(), page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Your listings fetched", response));
    }
}