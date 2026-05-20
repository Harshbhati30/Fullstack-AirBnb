package com.airbnb.backend.repository;

import com.airbnb.backend.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {



    Page<Review> findByPropertyIdOrderByCreatedAtDesc(Long propertyId, Pageable pageable);


    boolean existsByUserIdAndPropertyId(Long userId, Long propertyId);


    Optional<Review> findByIdAndUserId(Long reviewId, Long userId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r " +
            "WHERE r.property.id = :propertyId")
    Double calculateAverageRating(@Param("propertyId") Long propertyId);


    long countByPropertyId(Long propertyId);
}