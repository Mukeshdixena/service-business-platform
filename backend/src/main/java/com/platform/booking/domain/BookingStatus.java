package com.platform.booking.domain;

import java.util.EnumSet;
import java.util.Set;

public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CHECKED_IN,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    NO_SHOW,
    REJECTED;

    private static final Set<BookingStatus> NON_TERMINAL =
            EnumSet.of(PENDING, CONFIRMED, CHECKED_IN, IN_PROGRESS);

    /** Non-terminal bookings are the ones that block a time slot (see availability/overlap logic). */
    public boolean isNonTerminal() {
        return NON_TERMINAL.contains(this);
    }

    public static Set<BookingStatus> nonTerminalStatuses() {
        return NON_TERMINAL;
    }
}
