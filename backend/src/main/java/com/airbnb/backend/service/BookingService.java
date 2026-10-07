package com.airbnb.backend.service;

import com.airbnb.backend.dto.request.BookingRequest;
import com.airbnb.backend.dto.response.BookingResponse;
import com.airbnb.backend.dto.response.PagedResponse;
import com.airbnb.backend.dto.response.PaymentResponse;
import com.airbnb.backend.entity.Booking;
import com.airbnb.backend.entity.Payment;
import com.airbnb.backend.entity.Property;
import com.airbnb.backend.entity.User;
import com.airbnb.backend.enums.BookingStatus;
import com.airbnb.backend.enums.PaymentStatus;
import com.airbnb.backend.exception.BadRequestException;
import com.airbnb.backend.exception.ConflictException;
import com.airbnb.backend.exception.ResourceNotFoundException;
import com.airbnb.backend.repository.BookingRepository;
import com.airbnb.backend.repository.PropertyRepository;
import com.airbnb.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BookingService {

    private static final List<BookingStatus> BLOCKING_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);
    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.03");
    private static final int MAX_STAY_NIGHTS = 30;
    private static final long FULL_REFUND_MIN_DAYS_BEFORE_CHECKIN = 2;
    private static final int MAX_PAGE_SIZE = 50;

    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final PropertyService propertyService;
    private final UserService userService;
    private final PaymentService paymentService;

    @Value("${app.booking.pending-expiry-minutes:30}")
    private long pendingExpiryMinutes;

    @Transactional
    public BookingResponse createBooking(Long userId, BookingRequest request) {
        LocalDate checkIn = request.getCheckInDate();
        LocalDate checkOut = request.getCheckOutDate();

        if (!checkOut.isAfter(checkIn)) {
            throw new BadRequestException("Check-out date must be after check-in date");
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights > MAX_STAY_NIGHTS) {
            throw new BadRequestException("Maximum stay is " + MAX_STAY_NIGHTS + " nights");
        }

        Property property = propertyRepository.findByIdForUpdate(request.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Property", "id", request.getPropertyId()));

        if (!Boolean.TRUE.equals(property.getIsActive())) {
            throw new BadRequestException("This property is not available for booking");
        }
        if (property.getHost().getId().equals(userId)) {
            throw new BadRequestException("You cannot book your own property");
        }
        if (request.getGuests() > property.getMaxGuests()) {
            throw new BadRequestException(
                    "Number of guests exceeds property maximum of " + property.getMaxGuests());
        }

        if (propertyRepository.isPropertyBooked(property.getId(), checkIn, checkOut,
                BLOCKING_STATUSES)) {
            throw new ConflictException("Property is not available for the selected dates");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        BigDecimal totalAmount = property.getPricePerNight()
                .multiply(BigDecimal.valueOf(nights))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformFee = totalAmount.multiply(PLATFORM_FEE_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        Booking booking = Booking.builder()
                .user(user)
                .property(property)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .guests(request.getGuests())
                .totalAmount(totalAmount)
                .platformFee(platformFee)
                .specialRequests(request.getSpecialRequests())
                .status(BookingStatus.PENDING)
                .build();

        return mapToResponse(bookingRepository.save(booking));
    }

    public BookingResponse getBookingById(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        boolean isGuest = booking.getUser().getId().equals(userId);
        boolean isHost = booking.getProperty().getHost().getId().equals(userId);
        if (!isGuest && !isHost) {
            throw new ResourceNotFoundException("Booking", "id", bookingId);
        }
        return mapToResponse(booking);
    }

    public PagedResponse<BookingResponse> getUserBookings(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        return buildPagedResponse(
                bookingRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable));
    }

    public PagedResponse<BookingResponse> getHostBookings(Long hostId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        return buildPagedResponse(bookingRepository.findAllByHostId(hostId, pageable));
    }

    public PagedResponse<BookingResponse> getAllBookings(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size),
                org.springframework.data.domain.Sort.by("createdAt").descending());
        return buildPagedResponse(bookingRepository.findAll(pageable));
    }


    @Transactional
    public BookingResponse cancelBooking(Long userId, Long bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        boolean isGuest = booking.getUser().getId().equals(userId);
        boolean isHost = booking.getProperty().getHost().getId().equals(userId);
        if (!isGuest && !isHost) {
            throw new ResourceNotFoundException("Booking", "id", bookingId);
        }

        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.REJECTED) {
            throw new BadRequestException("Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel a completed booking");
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            LocalDate today = LocalDate.now();
            if (!today.isBefore(booking.getCheckInDate())) {
                throw new BadRequestException(
                        "This stay has already started and can no longer be cancelled");
            }

            Payment payment = booking.getPayment();
            if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
                long daysToCheckIn = ChronoUnit.DAYS.between(today, booking.getCheckInDate());
                if (isHost || daysToCheckIn >= FULL_REFUND_MIN_DAYS_BEFORE_CHECKIN) {
                    paymentService.refundFull(payment);   // throws (and rolls back) if it fails
                }
            }
        }

        String cleanReason = (reason == null || reason.isBlank()) ? null : reason.trim();
        if (cleanReason != null && cleanReason.length() > 450) {
            cleanReason = cleanReason.substring(0, 450);
        }
        if (isHost && !isGuest) {
            cleanReason = "Cancelled by host" + (cleanReason != null ? ": " + cleanReason : "");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(cleanReason);
        return mapToResponse(bookingRepository.save(booking));
    }

    @Transactional
    public int expireStalePendingBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(pendingExpiryMinutes);
        int count = bookingRepository.cancelStaleBookings(
                BookingStatus.PENDING, BookingStatus.CANCELLED,
                "Payment not completed in time", cutoff);
        if (count > 0) log.info("Expired {} unpaid bookings", count);
        return count;
    }


    @Transactional
    public int markFinishedStaysCompleted() {
        int count = bookingRepository.markStaysCompleted(
                BookingStatus.CONFIRMED, BookingStatus.COMPLETED, LocalDate.now());
        if (count > 0) log.info("Marked {} stays as completed", count);
        return count;
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
                .payment(mapPayment(booking.getPayment()))   // was always null before
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private PaymentResponse mapPayment(Payment payment) {
        return payment == null ? null : paymentService.mapToResponse(payment);
    }

    private int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private PagedResponse<BookingResponse> buildPagedResponse(Page<Booking> page) {
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