package com.airbnb.backend.dto.request;

import com.airbnb.backend.enums.PropertyType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PropertyRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 10, max = 200, message = "Title must be between 10 and 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 20, max = 5000, message = "Description must be between 20 and 5000 characters")
    private String description;

    @NotNull(message = "Price per night is required")
    @DecimalMin(value = "1.0", message = "Price must be at least 1")
    private BigDecimal pricePerNight;

    @NotNull(message = "Max guests is required")
    @Min(value = 1, message = "At least 1 guest must be allowed")
    @Max(value = 50, message = "Max guests cannot exceed 50")
    private Integer maxGuests;

    @Min(value = 0, message = "Bedrooms cannot be negative")
    private Integer bedrooms;

    @Min(value = 0, message = "Bathrooms cannot be negative")
    private Integer bathrooms;

    @NotNull(message = "Property type is required")
    private PropertyType propertyType;


    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Country is required")
    private String country;

    private String zipCode;
    private Double latitude;
    private Double longitude;


    private List<Long> amenityIds;
}