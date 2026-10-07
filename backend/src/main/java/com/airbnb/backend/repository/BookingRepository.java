package com.airbnb.backend.repository;

import com.airbnb.backend.entity.Booking;
import com.airbnb.backend.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
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
            Long userId, Long propertyId, List<BookingStatus> statuses);

    Optional<Booking> findByIdAndUserId(Long bookingId, Long userId);

    long countByStatus(BookingStatus status);


    boolean existsByPropertyIdAndStatusInAndCheckOutDateAfter(
            Long propertyId, Collection<BookingStatus> statuses, LocalDate date);

    @Query("SELECT b FROM Booking b WHERE b.property.id = :propertyId " +
            "AND b.status = com.airbnb.backend.enums.BookingStatus.CONFIRMED " +
            "AND b.checkOutDate >= :today " +
            "ORDER BY b.checkInDate ASC")
    List<Booking> findUpcomingBookings(@Param("propertyId") Long propertyId,
                                       @Param("today") LocalDate today);



    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.status IN :statuses")
    BigDecimal sumTotalAmountByStatuses(@Param("statuses") Collection<BookingStatus> statuses);

    @Query("SELECT COALESCE(SUM(b.platformFee), 0) FROM Booking b WHERE b.status IN :statuses")
    BigDecimal sumPlatformFeeByStatuses(@Param("statuses") Collection<BookingStatus> statuses);



    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.user.id = :userId " +
            "AND b.property.id = :propertyId " +
            "AND b.status IN :statuses " +
            "AND b.checkOutDate <= :today")
    boolean hasStayed(@Param("userId") Long userId,
                      @Param("propertyId") Long propertyId,
                      @Param("statuses") Collection<BookingStatus> statuses,
                      @Param("today") LocalDate today);



    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Booking b SET b.status = :newStatus, b.cancellationReason = :reason " +
            "WHERE b.status = :oldStatus AND b.createdAt < :cutoff")
    int cancelStaleBookings(@Param("oldStatus") BookingStatus oldStatus,
                            @Param("newStatus") BookingStatus newStatus,
                            @Param("reason") String reason,
                            @Param("cutoff") LocalDateTime cutoff);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Booking b SET b.status = :newStatus " +
            "WHERE b.status = :oldStatus AND b.checkOutDate <= :today")
    int markStaysCompleted(@Param("oldStatus") BookingStatus oldStatus,
                           @Param("newStatus") BookingStatus newStatus,
                           @Param("today") LocalDate today);
}