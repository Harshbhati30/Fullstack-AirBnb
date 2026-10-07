package com.airbnb.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {

    private long totalUsers;
    private long totalProperties;
    private long activeProperties;
    private long totalBookings;
    private Map<String, Long> bookingsByStatus;
    private BigDecimal totalRevenue;
    private BigDecimal platformEarnings;
}