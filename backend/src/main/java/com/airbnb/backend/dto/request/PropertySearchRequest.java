package com.airbnb.backend.dto.request;

import com.airbnb.backend.enums.PropertyType;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


@Data
public class PropertySearchRequest {

    private String location;
    private String city;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer guests;
    private PropertyType propertyType;
    private Double minRating;
    private List<Long> amenityIds;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkIn;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOut;
}