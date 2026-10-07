package com.airbnb.backend.service;

import com.airbnb.backend.dto.response.PagedResponse;
import com.airbnb.backend.dto.response.PropertyResponse;
import com.airbnb.backend.entity.Property;
import com.airbnb.backend.entity.User;
import com.airbnb.backend.entity.Wishlist;
import com.airbnb.backend.exception.ResourceNotFoundException;
import com.airbnb.backend.repository.PropertyRepository;
import com.airbnb.backend.repository.UserRepository;
import com.airbnb.backend.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final PropertyService propertyService;


    @Transactional
    public boolean toggleWishlist(Long userId, Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", "id", propertyId));

        var existing = wishlistRepository.findByUserIdAndPropertyId(userId, propertyId);
        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            return false;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        wishlistRepository.save(Wishlist.builder().user(user).property(property).build());
        return true;
    }

    public PagedResponse<PropertyResponse> getUserWishlist(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        Page<Wishlist> wishlists = wishlistRepository.findActiveByUserId(userId, pageable);

        List<PropertyResponse> content = wishlists.getContent().stream()
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


    public List<Long> getWishlistedPropertyIds(Long userId) {
        return wishlistRepository.findPropertyIdsByUserId(userId);
    }

    public boolean isWishlisted(Long userId, Long propertyId) {
        return wishlistRepository.existsByUserIdAndPropertyId(userId, propertyId);
    }
}