package com.airbnb.backend.service;

import com.airbnb.backend.dto.request.BookingRequest;
import com.airbnb.backend.dto.response.BookingResponse;
import com.airbnb.backend.dto.response.PagedResponse;
import com.airbnb.backend.entity.*;
import com.airbnb.backend.enums.BookingStatus;
import com.airbnb.backend.exception.BadRequestException;
import com.airbnb.backend.exception.ResourceNotFoundException;
import com.airbnb.backend.exception.UnauthorizedException;
import com.airbnb.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final PropertyService propertyService;
    private final UserService userService;

    @Transactional
    public BookingResponse createBooking(Long userId,
                                         BookingRequest request) {
        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new BadRequestException(
                    "Check-out date must be after check-in date");
        }

        Property property = propertyRepository
                .findById(request.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Property", "id", request.getPropertyId()));

        if (property.getHost().getId().equals(userId)) {
            throw new BadRequestException(
                    "You cannot book your own property");
        }

        if (request.getGuests() > property.getMaxGuests()) {
            throw new BadRequestException(
                    "Number of guests exceeds property maximum of "
                            + property.getMaxGuests());
        }

        boolean isBooked = propertyRepository.isPropertyBooked(
                property.getId(),
                request.getCheckInDate(),
                request.getCheckOutDate()
        );
        if (isBooked) {
            throw new BadRequestException(
                    "Property is not available for the selected dates");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "id", userId));

        long nights = ChronoUnit.DAYS.between(
                request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal totalAmount = property.getPricePerNight()
                .multiply(BigDecimal.valueOf(nights));

        BigDecimal platformFee = totalAmount
                .multiply(new BigDecimal("0.03"))
                .setScale(2, RoundingMode.HALF_UP);

        Booking booking = Booking.builder()
                .user(user)
                .property(property)
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .guests(request.getGuests())
                .totalAmount(totalAmount)
                .platformFee(platformFee)
                .specialRequests(request.getSpecialRequests())
                .status(BookingStatus.PENDING)
                .build();

        return mapToResponse(bookingRepository.save(booking));
    }

    public BookingResponse getBookingById(Long userId, Long bookingId) {
        Booking booking = bookingRepository
                .findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking", "id", bookingId));
        return mapToResponse(booking);
    }

    public PagedResponse<BookingResponse> getUserBookings(
            Long userId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Booking> bookings = bookingRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return buildPagedResponse(bookings);
    }

    public PagedResponse<BookingResponse> getHostBookings(
            Long hostId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Booking> bookings = bookingRepository
                .findAllByHostId(hostId, pageable);
        return buildPagedResponse(bookings);
    }

    @Transactional
    public BookingResponse cancelBooking(Long userId, Long bookingId,
                                         String reason) {
        Booking booking = bookingRepository
                .findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking", "id", bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel a completed booking");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        return mapToResponse(bookingRepository.save(booking));
    }


    public BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .guests(booking.getGuests())
                .totalAmount(booking.getTotalAmount())
                .platformFee(booking.getPlatformFee())
                .status(booking.getStatus())
                .specialRequests(booking.getSpecialRequests())
                .cancellationReason(booking.getCancellationReason())
                .property(propertyService.mapToResponse(booking.getProperty()))
                .user(userService.mapToResponse(booking.getUser()))
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private PagedResponse<BookingResponse> buildPagedResponse(
            Page<Booking> page) {
        List<BookingResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PagedResponse.<BookingResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}