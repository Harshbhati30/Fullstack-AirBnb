package com.airbnb.backend.service;

import com.airbnb.backend.dto.response.PagedResponse;
import com.airbnb.backend.dto.response.PropertyResponse;
import com.airbnb.backend.entity.*;
import com.airbnb.backend.exception.BadRequestException;
import com.airbnb.backend.exception.ResourceNotFoundException;
import com.airbnb.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final PropertyService propertyService;

    @Transactional
    public void toggleWishlist(Long userId, Long propertyId) {
        propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Property", "id", propertyId));

        if (wishlistRepository.existsByUserIdAndPropertyId(userId, propertyId)) {
            wishlistRepository.findByUserIdAndPropertyId(userId, propertyId)
                    .ifPresent(wishlistRepository::delete);
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User", "id", userId));
            Property property = propertyRepository.findById(propertyId)
                    .orElseThrow();

            wishlistRepository.save(Wishlist.builder()
                    .user(user)
                    .property(property)
                    .build());
        }
    }

    public PagedResponse<PropertyResponse> getUserWishlist(
            Long userId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Wishlist> wishlists = wishlistRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<PropertyResponse> content = wishlists.getContent()
                .stream()
                .map(w -> propertyService.mapToResponse(w.getProperty()))
                .collect(Collectors.toList());

        return PagedResponse.<PropertyResponse>builder()
                .content(content)
                .pageNumber(wishlists.getNumber())
                .pageSize(wishlists.getSize())
                .totalElements(wishlists.getTotalElements())
                .totalPages(wishlists.getTotalPages())
                .last(wishlists.isLast())
                .first(wishlists.isFirst())
                .build();
    }

    public boolean isWishlisted(Long userId, Long propertyId) {
        return wishlistRepository
                .existsByUserIdAndPropertyId(userId, propertyId);
    }
}