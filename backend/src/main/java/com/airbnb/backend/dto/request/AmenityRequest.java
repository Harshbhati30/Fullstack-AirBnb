package com.airbnb.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AmenityRequest {

    @NotBlank(message = "Amenity name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @Size(max = 100, message = "Icon cannot exceed 100 characters")
    private String icon;
}