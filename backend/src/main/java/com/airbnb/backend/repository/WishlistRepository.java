package com.airbnb.backend.repository;

import com.airbnb.backend.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {


    Page<Wishlist> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    boolean existsByUserIdAndPropertyId(Long userId, Long propertyId);
    Optional<Wishlist> findByUserIdAndPropertyId(Long userId, Long propertyId);

    @Query("SELECT w FROM Wishlist w WHERE w.user.id = :userId " +
            "AND w.property.isActive = true ORDER BY w.createdAt DESC")
    Page<Wishlist> findActiveByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT w.property.id FROM Wishlist w WHERE w.user.id = :userId")
    List<Long> findPropertyIdsByUserId(@Param("userId") Long userId);
}