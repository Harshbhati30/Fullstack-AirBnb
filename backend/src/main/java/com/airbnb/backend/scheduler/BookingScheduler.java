package com.airbnb.backend.scheduler;

import com.airbnb.backend.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingScheduler {

    private final BookingService bookingService;


    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void expireUnpaidBookings() {
        try {
            bookingService.expireStalePendingBookings();
        } catch (Exception e) {
            log.error("Failed to expire unpaid bookings", e);
        }
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 30_000)
    public void completeFinishedStays() {
        try {
            bookingService.markFinishedStaysCompleted();
        } catch (Exception e) {
            log.error("Failed to complete finished stays", e);
        }
    }
}