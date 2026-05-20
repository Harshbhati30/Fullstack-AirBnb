package com.airbnb.backend.repository;

import com.airbnb.backend.entity.Booking;
import com.airbnb.backend.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {


    Page<Booking> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Booking> findByPropertyIdOrderByCreatedAtDesc(Long propertyId, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.property.host.id = :hostId " +
            "ORDER BY b.createdAt DESC")
    Page<Booking> findAllByHostId(@Param("hostId") Long hostId, Pageable pageable);


    boolean existsByUserIdAndPropertyIdAndStatusIn(
            Long userId, Long propertyId, List<BookingStatus> statuses
    );


    Optional<Booking> findByIdAndUserId(Long bookingId, Long userId);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b " +
            "WHERE b.status = 'CONFIRMED'")
    BigDecimal calculateTotalRevenue();


    long countByStatus(BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.property.id = :propertyId " +
            "AND b.status = 'CONFIRMED' " +
            "AND b.checkInDate >= :today " +
            "ORDER BY b.checkInDate ASC")
    List<Booking> findUpcomingBookings(
            @Param("propertyId") Long propertyId,
            @Param("today") LocalDate today
    );
}