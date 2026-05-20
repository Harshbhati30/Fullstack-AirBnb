
package com.airbnb.backend.enums;

public enum BookingStatus {
    PENDING,      // booking created, payment not yet done
    CONFIRMED,    // payment successful
    CANCELLED,    // cancelled by user or host
    COMPLETED,    // stay is over
    REJECTED
}