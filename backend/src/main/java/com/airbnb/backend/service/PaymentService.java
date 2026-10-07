package com.airbnb.backend.service;

import com.airbnb.backend.dto.request.PaymentVerificationRequest;
import com.airbnb.backend.dto.response.PaymentOrderResponse;
import com.airbnb.backend.dto.response.PaymentResponse;
import com.airbnb.backend.entity.Booking;
import com.airbnb.backend.entity.Payment;
import com.airbnb.backend.enums.BookingStatus;
import com.airbnb.backend.enums.PaymentStatus;
import com.airbnb.backend.exception.BadRequestException;
import com.airbnb.backend.exception.ResourceNotFoundException;
import com.airbnb.backend.repository.BookingRepository;
import com.airbnb.backend.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Transactional
    public PaymentOrderResponse createOrder(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only bookings awaiting payment can be paid");
        }

        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new BadRequestException("This booking is already paid");
        }

        long amountInPaise = toPaise(booking.getTotalAmount());

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "booking_" + bookingId);

            Order order = razorpayClient.orders.create(orderRequest);
            String orderId = order.get("id");

            if (payment == null) {
                payment = Payment.builder()
                        .booking(booking)
                        .amount(booking.getTotalAmount())
                        .build();
            }

            payment.setRazorpayOrderId(orderId);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setFailureReason(null);
            paymentRepository.save(payment);

            log.info("Razorpay order {} created for booking {}", orderId, bookingId);

            return PaymentOrderResponse.builder()
                    .orderId(orderId)
                    .amount(amountInPaise)
                    .currency("INR")
                    .keyId(keyId)
                    .bookingId(bookingId)
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new BadRequestException("Could not start the payment. Please try again.");
        }
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public PaymentResponse verifyPayment(Long userId, PaymentVerificationRequest request) {
        Booking booking = bookingRepository.findByIdAndUserId(request.getBookingId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking", "id", request.getBookingId()));

        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment", "orderId", request.getRazorpayOrderId()));

        if (!payment.getBooking().getId().equals(booking.getId())) {
            throw new BadRequestException("This payment does not belong to the booking");
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS
                || payment.getStatus() == PaymentStatus.REFUNDED) {
            return mapToResponse(payment);
        }

        boolean valid = isSignatureValid(request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(), request.getRazorpaySignature());

        if (!valid) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Signature verification failed");
            paymentRepository.save(payment);
            log.warn("Invalid Razorpay signature for booking {}", booking.getId());
            throw new BadRequestException("Payment verification failed");
        }

        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());

        if (booking.getStatus() != BookingStatus.PENDING) {

            paymentRepository.save(payment);
            refundFull(payment);
            log.warn("Booking {} was no longer pending; payment refunded", booking.getId());
            return mapToResponse(payment);   // status = REFUNDED
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        paymentRepository.save(payment);
        log.info("Payment verified, booking {} confirmed", booking.getId());

        return mapToResponse(payment);
    }

    @Transactional
    public void refundFull(Payment payment) {
        if (payment == null || payment.getStatus() != PaymentStatus.SUCCESS) return;

        try {
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", toPaise(payment.getAmount()));
            refundRequest.put("speed", "normal");

            razorpayClient.payments.refund(payment.getRazorpayPaymentId(), refundRequest);

            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
            log.info("Refund issued for payment {}", payment.getRazorpayPaymentId());

        } catch (RazorpayException e) {
            log.error("Razorpay refund failed: {}", e.getMessage());
            throw new BadRequestException(
                    "The refund could not be processed right now. Please try again later.");
        }
    }

    public PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .build();
    }

    private long toPaise(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    private boolean isSignatureValid(String orderId, String paymentId, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(hash);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            log.error("Signature check error: {}", e.getMessage());
            return false;
        }
    }
}