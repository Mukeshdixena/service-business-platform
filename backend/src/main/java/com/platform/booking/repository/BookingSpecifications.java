package com.platform.booking.repository;

import com.platform.booking.domain.Booking;
import com.platform.booking.domain.BookingStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

/** Predicate builders for the filtered booking-list queries — see {@link BookingRepository}. */
public final class BookingSpecifications {

    private BookingSpecifications() {
    }

    public static Specification<Booking> businessId(UUID businessId) {
        return (root, query, cb) -> cb.equal(root.get("businessId"), businessId);
    }

    public static Specification<Booking> customerId(UUID customerId) {
        return (root, query, cb) -> cb.equal(root.get("customerId"), customerId);
    }

    public static Specification<Booking> status(BookingStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Booking> startAtFrom(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startAt"), from);
    }

    public static Specification<Booking> startAtTo(Instant to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("startAt"), to);
    }
}
