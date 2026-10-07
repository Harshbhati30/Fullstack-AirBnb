package com.airbnb.backend.service;

import com.airbnb.backend.dto.request.ReviewRequest;
import com.airbnb.backend.dto.response.PagedResponse;
import com.airbnb.backend.dto.response.ReviewResponse;
import com.airbnb.backend.entity.Property;
import com.airbnb.backend.entity.Review;
import com.airbnb.backend.entity.User;
import com.airbnb.backend.enums.BookingStatus;
import com.airbnb.backend.exception.BadRequestException;
import com.airbnb.backend.exception.ResourceNotFoundException;
import com.airbnb.backend.repository.BookingRepository;
import com.airbnb.backend.repository.PropertyRepository;
import com.airbnb.backend.repository.ReviewRepository;
import com.airbnb.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private static final List<BookingStatus> STAY_STATUSES =
            List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED);

    private final ReviewRepository reviewRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final UserService userService;

    @Transactional
    public ReviewResponse createReview(Long userId, ReviewRequest request) {
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Property", "id", request.getPropertyId()));


        boolean hasStayed = bookingRepository.hasStayed(
                userId, property.getId(), STAY_STATUSES, LocalDate.now());
        if (!hasStayed) {
            throw new BadRequestException(
                    "You can only review a property after completing a stay there");
        }

        if (reviewRepository.existsByUserIdAndPropertyId(userId, property.getId())) {
            throw new BadRequestException("You have already reviewed this property");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Review review = Review.builder()
                .rating(request.getRating())
                .comment(request.getComment())
                .user(user)
                .property(property)
                .build();

        reviewRepository.saveAndFlush(review);
        updatePropertyRating(property);
        return mapToResponse(review);
    }

    public PagedResponse<ReviewResponse> getPropertyReviews(Long propertyId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        Page<Review> reviews = reviewRepository
                .findByPropertyIdOrderByCreatedAtDesc(propertyId, pageable);

        List<ReviewResponse> content = reviews.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PagedResponse.<ReviewResponse>builder()
                .content(content)
                .pageNumber(reviews.getNumber())
                .pageSize(reviews.getSize())
                .totalElements(reviews.getTotalElements())
                .totalPages(reviews.getTotalPages())
                .last(reviews.isLast())
                .first(reviews.isFirst())
                .build();
    }

    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
        removeReview(review);
    }


    @Transactional
    public void deleteReviewAsAdmin(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
        removeReview(review);
    }

    private void removeReview(Review review) {
        Property property = review.getProperty();
        reviewRepository.delete(review);
        reviewRepository.flush();
        updatePropertyRating(property);
    }

    private void updatePropertyRating(Property property) {
        Double avg = reviewRepository.calculateAverageRating(property.getId());
        long count = reviewRepository.countByPropertyId(property.getId());

        property.setAverageRating(
                BigDecimal.valueOf(avg == null ? 0.0 : avg).setScale(2, RoundingMode.HALF_UP));
        property.setTotalReviews((int) count);
        propertyRepository.save(property);
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .user(userService.mapToPublicResponse(review.getUser()))
                .createdAt(review.getCreatedAt())
                .build();
    }
}