package com.airbnb.backend.repository;

import com.airbnb.backend.entity.Payment;
import com.airbnb.backend.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByBookingId(Long bookingId);
    boolean existsByBookingIdAndStatus(Long bookingId, PaymentStatus status);
}