package com.platform.booking;

import com.platform.booking.domain.Booking;
import com.platform.booking.domain.BookingConflictDetector;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CLAUDE_CODE.md §34: {@code requestedStart < existingEnd AND requestedEnd >
 * existingStart} means overlap. The same rule must apply to a resource's rental
 * calendar as to a staff member's appointment calendar.
 */
class BookingConflictDetectorTest {

    private static final UUID BUSINESS = UUID.randomUUID();
    private static final UUID CUSTOMER = UUID.randomUUID();
    private static final UUID SERVICE = UUID.randomUUID();
    private static final UUID JCB_1 = UUID.randomUUID();
    private static final UUID JCB_2 = UUID.randomUUID();
    private static final UUID STAFF_1 = UUID.randomUUID();

    private static Instant at(String iso) {
        return Instant.parse(iso);
    }

    private Booking rental(UUID resourceId, String start, String end) {
        return new Booking(BUSINESS, CUSTOMER, SERVICE, null, resourceId, at(start), at(end),
                BigDecimal.TEN, "INR", null);
    }

    private Booking appointment(UUID staffId, String start, String end) {
        return new Booking(BUSINESS, CUSTOMER, SERVICE, staffId, at(start), at(end), BigDecimal.TEN, "INR", null);
    }

    @Test
    void detectsAnOverlappingRentalOnTheSameResource() {
        List<Booking> existing = List.of(rental(JCB_1, "2026-09-10T09:00:00Z", "2026-09-10T17:00:00Z"));

        assertThat(BookingConflictDetector.conflicts(at("2026-09-10T16:00:00Z"), at("2026-09-10T18:00:00Z"),
                null, JCB_1, existing)).isTrue();
    }

    @Test
    void detectsAFullyContainedAndAFullyContainingRequest() {
        List<Booking> existing = List.of(rental(JCB_1, "2026-09-10T09:00:00Z", "2026-09-10T17:00:00Z"));

        assertThat(BookingConflictDetector.conflicts(at("2026-09-10T10:00:00Z"), at("2026-09-10T11:00:00Z"),
                null, JCB_1, existing)).isTrue();
        assertThat(BookingConflictDetector.conflicts(at("2026-09-10T08:00:00Z"), at("2026-09-10T20:00:00Z"),
                null, JCB_1, existing)).isTrue();
    }

    @Test
    void backToBackRentalsDoNotConflict() {
        List<Booking> existing = List.of(rental(JCB_1, "2026-09-10T09:00:00Z", "2026-09-10T17:00:00Z"));

        // ends exactly when the existing one starts
        assertThat(BookingConflictDetector.conflicts(at("2026-09-10T07:00:00Z"), at("2026-09-10T09:00:00Z"),
                null, JCB_1, existing)).isFalse();
        // starts exactly when the existing one ends
        assertThat(BookingConflictDetector.conflicts(at("2026-09-10T17:00:00Z"), at("2026-09-10T19:00:00Z"),
                null, JCB_1, existing)).isFalse();
    }

    @Test
    void anIdenticalWindowOnADifferentResourceDoesNotConflict() {
        List<Booking> existing = List.of(rental(JCB_1, "2026-09-10T09:00:00Z", "2026-09-10T17:00:00Z"));

        assertThat(BookingConflictDetector.conflicts(at("2026-09-10T09:00:00Z"), at("2026-09-10T17:00:00Z"),
                null, JCB_2, existing)).isFalse();
    }

    @Test
    void aStaffAppointmentDoesNotBlockAResourceRentalAndViceVersa() {
        List<Booking> staffBooking = List.of(appointment(STAFF_1, "2026-09-10T09:00:00Z", "2026-09-10T10:00:00Z"));
        assertThat(BookingConflictDetector.conflicts(at("2026-09-10T09:00:00Z"), at("2026-09-10T10:00:00Z"),
                null, JCB_1, staffBooking)).isFalse();

        List<Booking> resourceBooking = List.of(rental(JCB_1, "2026-09-10T09:00:00Z", "2026-09-10T10:00:00Z"));
        assertThat(BookingConflictDetector.conflicts(at("2026-09-10T09:00:00Z"), at("2026-09-10T10:00:00Z"),
                STAFF_1, null, resourceBooking)).isFalse();
    }

    @Test
    void existingStaffOverlapIsStillDetectedForAppointments() {
        List<Booking> existing = List.of(appointment(STAFF_1, "2026-09-10T09:00:00Z", "2026-09-10T10:00:00Z"));

        assertThat(BookingConflictDetector.conflicts(at("2026-09-10T09:30:00Z"), at("2026-09-10T10:30:00Z"),
                STAFF_1, null, existing)).isTrue();
        assertThat(BookingConflictDetector.conflicts(at("2026-09-10T09:30:00Z"), at("2026-09-10T10:30:00Z"),
                UUID.randomUUID(), null, existing)).isFalse();
    }

    @Test
    void noExistingBookingsMeansNoConflict() {
        assertThat(BookingConflictDetector.conflicts(at("2026-09-10T09:00:00Z"), at("2026-09-10T10:00:00Z"),
                null, JCB_1, List.of())).isFalse();
    }
}
