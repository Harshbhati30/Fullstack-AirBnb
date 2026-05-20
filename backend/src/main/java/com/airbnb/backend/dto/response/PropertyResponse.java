package com.airbnb.backend.dto.response;

import com.airbnb.backend.enums.PropertyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyResponse {

    private Long id;
    private String title;
    private String description;
    private BigDecimal pricePerNight;
    private Integer maxGuests;
    private Integer bedrooms;
    private Integer bathrooms;
    private String city;
    private PropertyType propertyType;
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Boolean isActive;

    // Nested response objects — not flat IDs
    private AddressResponse address;
    private UserResponse host;
    private List<PropertyImageResponse> images;
    private Set<AmenityResponse> amenities;

    private LocalDateTime createdAt;
}