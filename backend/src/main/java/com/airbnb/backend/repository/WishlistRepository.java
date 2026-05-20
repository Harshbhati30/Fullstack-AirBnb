package com.airbnb.backend.repository;

import com.airbnb.backend.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {


    Page<Wishlist> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    boolean existsByUserIdAndPropertyId(Long userId, Long propertyId);
    Optional<Wishlist> findByUserIdAndPropertyId(Long userId, Long propertyId);
}