package com.airbnb.backend.service;

import com.airbnb.backend.dto.request.ReviewRequest;
import com.airbnb.backend.dto.response.PagedResponse;
import com.airbnb.backend.dto.response.ReviewResponse;
import com.airbnb.backend.entity.*;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public ReviewResponse createReview(Long userId, ReviewRequest request) {
        if (reviewRepository.existsByUserIdAndPropertyId(
                userId, request.getPropertyId())) {
            throw new BadRequestException(
                    "You have already reviewed this property");
        }

        Property property = propertyRepository
                .findById(request.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Property", "id", request.getPropertyId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "id", userId));

        Review review = Review.builder()
                .rating(request.getRating())
                .comment(request.getComment())
                .user(user)
                .property(property)
                .build();

        reviewRepository.save(review);
        updatePropertyRating(property);
        return mapToResponse(review);
    }

    public PagedResponse<ReviewResponse> getPropertyReviews(
            Long propertyId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviews = reviewRepository
                .findByPropertyIdOrderByCreatedAtDesc(propertyId, pageable);

        List<ReviewResponse> content = reviews.getContent()
                .stream().map(this::mapToResponse)
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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Review", "id", reviewId));

        Property property = review.getProperty();
        reviewRepository.delete(review);
        updatePropertyRating(property);
    }

    private void updatePropertyRating(Property property) {
        Double avg = reviewRepository
                .calculateAverageRating(property.getId());
        long count = reviewRepository.countByPropertyId(property.getId());

        property.setAverageRating(
                BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        property.setTotalReviews((int) count);
        propertyRepository.save(property);
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .user(userService.mapToResponse(review.getUser()))
                .createdAt(review.getCreatedAt())
                .build();
    }
}