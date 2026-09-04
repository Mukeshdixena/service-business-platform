package com.platform.booking.domain;

import com.platform.availability.service.TimeRangeUtil;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

/**
 * The single implementation of CLAUDE_CODE.md §33/§34's overlap rule
 * ({@code requestedStart < existingEnd AND requestedEnd > existingStart}),
 * applied identically whether the booking is keyed on a staff member
 * (APPOINTMENT) or a resource (RENTAL).
 *
 * <p>Pure and static so the rule can be unit-tested directly, and so there is no
 * second, subtly-different copy of it for rentals.
 */
public final class BookingConflictDetector {

    private BookingConflictDetector() {
    }

    /**
     * @param staffId    the staff member the request is keyed on, or null
     * @param resourceId the resource the request is keyed on, or null
     * @param candidates existing non-terminal bookings for the business in the window
     */
    public static boolean conflicts(Instant startAt, Instant endAt, UUID staffId, UUID resourceId,
                                     Collection<Booking> candidates) {
        return candidates.stream()
                .filter(b -> sameTrack(b, staffId, resourceId))
                .anyMatch(b -> TimeRangeUtil.overlaps(startAt, endAt, b.getStartAt(), b.getEndAt()));
    }

    /**
     * A rental request only conflicts with bookings on the same resource; an
     * appointment request only with bookings for the same staff member (where
     * "no staff assigned" is itself a shared calendar, matching the existing
     * appointment behaviour).
     */
    private static boolean sameTrack(Booking existing, UUID staffId, UUID resourceId) {
        if (resourceId != null) {
            return Objects.equals(existing.getResourceId(), resourceId);
        }
        return existing.getResourceId() == null && Objects.equals(existing.getStaffId(), staffId);
    }
}
