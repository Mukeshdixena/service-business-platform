package com.platform.booking.domain;

import com.platform.common.exception.InvalidStateTransitionException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The explicit, server-enforced booking lifecycle from API_CONTRACT.md. There is
 * no generic "update status" endpoint — each lifecycle action (confirm,
 * check-in, ...) maps to exactly one target status, and any transition not
 * listed here is rejected with 409 INVALID_STATE_TRANSITION.
 *
 * <pre>
 * PENDING     -&gt; CONFIRMED, REJECTED, CANCELLED
 * CONFIRMED   -&gt; CHECKED_IN, CANCELLED, NO_SHOW
 * CHECKED_IN  -&gt; IN_PROGRESS, CANCELLED
 * IN_PROGRESS -&gt; COMPLETED
 * </pre>
 */
public final class BookingStateMachine {

    private static final Map<BookingStatus, Set<BookingStatus>> TRANSITIONS = new EnumMap<>(BookingStatus.class);

    static {
        TRANSITIONS.put(BookingStatus.PENDING,
                EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.REJECTED, BookingStatus.CANCELLED));
        TRANSITIONS.put(BookingStatus.CONFIRMED,
                EnumSet.of(BookingStatus.CHECKED_IN, BookingStatus.CANCELLED, BookingStatus.NO_SHOW));
        TRANSITIONS.put(BookingStatus.CHECKED_IN,
                EnumSet.of(BookingStatus.IN_PROGRESS, BookingStatus.CANCELLED));
        TRANSITIONS.put(BookingStatus.IN_PROGRESS,
                EnumSet.of(BookingStatus.COMPLETED));
        TRANSITIONS.put(BookingStatus.COMPLETED, EnumSet.noneOf(BookingStatus.class));
        TRANSITIONS.put(BookingStatus.CANCELLED, EnumSet.noneOf(BookingStatus.class));
        TRANSITIONS.put(BookingStatus.NO_SHOW, EnumSet.noneOf(BookingStatus.class));
        TRANSITIONS.put(BookingStatus.REJECTED, EnumSet.noneOf(BookingStatus.class));
    }

    private BookingStateMachine() {
    }

    public static boolean canTransition(BookingStatus from, BookingStatus to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public static void requireTransition(BookingStatus from, BookingStatus to) {
        if (!canTransition(from, to)) {
            throw new InvalidStateTransitionException(
                    "Cannot transition booking from " + from + " to " + to + ".");
        }
    }
}
