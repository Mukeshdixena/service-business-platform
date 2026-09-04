package com.platform.catalog.domain;

/**
 * Only {@code APPOINTMENT} has working availability/booking logic in this pass
 * (see API_CONTRACT.md). The others are accepted and stored for forward
 * compatibility with the Queue/Rental/Classes phases.
 */
public enum ServiceBookingType {
    APPOINTMENT,
    QUEUE,
    REQUEST,
    RENTAL,
    WALK_IN
}
