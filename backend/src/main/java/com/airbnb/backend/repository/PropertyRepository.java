package com.airbnb.backend.repository;

import com.airbnb.backend.entity.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long>,
        JpaSpecificationExecutor<Property> {

    Page<Property> findByHostIdAndIsActiveTrue(Long hostId, Pageable pageable);

    Page<Property> findByHostId(Long hostId, Pageable pageable);


    @Query("SELECT p FROM Property p WHERE p.isActive = true " +
            "AND LOWER(p.city) = LOWER(:city)")
    Page<Property> findByCityIgnoreCase(@Param("city") String city, Pageable pageable);


    @Query("SELECT p FROM Property p WHERE p.isActive = true " +
            "ORDER BY p.averageRating DESC")
    List<Property> findTopRatedProperties(Pageable pageable);


    long countByIsActiveTrue();


    @Query("SELECT COUNT(b) > 0 FROM Booking b " +
            "WHERE b.property.id = :propertyId " +
            "AND b.status IN ('CONFIRMED', 'PENDING') " +
            "AND b.checkInDate < :checkOut " +
            "AND b.checkOutDate > :checkIn")
    boolean isPropertyBooked(
            @Param("propertyId") Long propertyId,
            @Param("checkIn") java.time.LocalDate checkIn,
            @Param("checkOut") java.time.LocalDate checkOut
    );
}