package com.airbnb.backend.controller;

import com.airbnb.backend.dto.response.AmenityResponse;
import com.airbnb.backend.service.AmenityService;
import com.airbnb.backend.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/amenities")
@RequiredArgsConstructor
public class AmenityController {

    private final AmenityService amenityService;


    @GetMapping
    public ResponseEntity<ApiResponse<List<AmenityResponse>>> getAllAmenities() {
        return ResponseEntity.ok(ApiResponse.success(
                "Amenities fetched", amenityService.getAllAmenities()));
    }
}