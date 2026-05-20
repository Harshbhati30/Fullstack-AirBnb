package com.airbnb.backend.dto.response;

import com.airbnb.backend.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer guests;
    private BigDecimal totalAmount;
    private BigDecimal platformFee;
    private BookingStatus status;
    private String specialRequests;
    private String cancellationReason;


    private PropertyResponse property;
    private UserResponse user;
    private PaymentResponse payment;

    private LocalDateTime createdAt;
}