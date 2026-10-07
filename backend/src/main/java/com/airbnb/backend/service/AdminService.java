package com.airbnb.backend.service;

import com.airbnb.backend.dto.request.AmenityRequest;
import com.airbnb.backend.dto.response.*;
import com.airbnb.backend.enums.BookingStatus;
import com.airbnb.backend.repository.BookingRepository;
import com.airbnb.backend.repository.PropertyRepository;
import com.airbnb.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private static final List<BookingStatus> REVENUE_STATUSES =
            List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED);

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final PropertyService propertyService;
    private final BookingService bookingService;
    private final ReviewService reviewService;
    private final AmenityService amenityService;

    public AdminStatsResponse getStats() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (BookingStatus status : BookingStatus.values()) {
            byStatus.put(status.name(), bookingRepository.countByStatus(status));
        }

        return AdminStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalProperties(propertyRepository.count())
                .activeProperties(propertyRepository.countByIsActiveTrue())
                .totalBookings(bookingRepository.count())
                .bookingsByStatus(byStatus)
                .totalRevenue(bookingRepository.sumTotalAmountByStatuses(REVENUE_STATUSES))
                .platformEarnings(bookingRepository.sumPlatformFeeByStatuses(REVENUE_STATUSES))
                .build();
    }

    public PagedResponse<UserResponse> getUsers(int page, int size) {
        return userService.getAllUsers(page, size);
    }

    @Transactional
    public UserResponse setUserActive(Long adminId, Long userId, boolean active) {
        return userService.setUserActive(adminId, userId, active);
    }

    public PagedResponse<PropertyResponse> getProperties(int page, int size) {
        return propertyService.getAllPropertiesForAdmin(page, size);
    }

    @Transactional
    public PropertyResponse setPropertyActive(Long adminId, Long propertyId, boolean active) {
        return propertyService.setActive(adminId, true, propertyId, active);
    }

    public PagedResponse<BookingResponse> getBookings(int page, int size) {
        return bookingService.getAllBookings(page, size);
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        reviewService.deleteReviewAsAdmin(reviewId);
    }

    @Transactional
    public AmenityResponse createAmenity(AmenityRequest request) {
        return amenityService.createAmenity(request);
    }
}