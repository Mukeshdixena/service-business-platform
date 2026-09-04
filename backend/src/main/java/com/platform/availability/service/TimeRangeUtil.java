package com.platform.availability.service;

import java.time.Instant;

/**
 * Shared overlap arithmetic (CLAUDE_CODE.md §33/§34), used by both availability
 * slot generation and booking-conflict checks so the "what counts as an
 * overlap" rule is defined exactly once.
 */
public final class TimeRangeUtil {

    private TimeRangeUtil() {
    }

    /** Two half-open intervals [startA, endA) and [startB, endB) overlap iff each starts before the other ends. */
    public static boolean overlaps(Instant startA, Instant endA, Instant startB, Instant endB) {
        return startA.isBefore(endB) && endA.isAfter(startB);
    }
}
