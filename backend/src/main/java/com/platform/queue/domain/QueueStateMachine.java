package com.platform.queue.domain;

import com.platform.common.exception.InvalidStateTransitionException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The explicit, server-enforced queue lifecycle from API_CONTRACT.md /
 * CLAUDE_CODE.md §15-16. Mirrors {@link com.platform.booking.domain.BookingStateMachine}'s
 * structure: each lifecycle action maps to exactly one target status, and any
 * transition not listed here is rejected with 409 INVALID_STATE_TRANSITION.
 *
 * <pre>
 * WAITING -&gt; CALLED, SKIPPED, CANCELLED
 * CALLED  -&gt; SERVING, NO_SHOW, CANCELLED
 * SERVING -&gt; COMPLETED
 * </pre>
 */
public final class QueueStateMachine {

    private static final Map<QueueEntryStatus, Set<QueueEntryStatus>> TRANSITIONS = new EnumMap<>(QueueEntryStatus.class);

    static {
        TRANSITIONS.put(QueueEntryStatus.WAITING,
                EnumSet.of(QueueEntryStatus.CALLED, QueueEntryStatus.SKIPPED, QueueEntryStatus.CANCELLED));
        TRANSITIONS.put(QueueEntryStatus.CALLED,
                EnumSet.of(QueueEntryStatus.SERVING, QueueEntryStatus.NO_SHOW, QueueEntryStatus.CANCELLED));
        TRANSITIONS.put(QueueEntryStatus.SERVING,
                EnumSet.of(QueueEntryStatus.COMPLETED));
        TRANSITIONS.put(QueueEntryStatus.COMPLETED, EnumSet.noneOf(QueueEntryStatus.class));
        TRANSITIONS.put(QueueEntryStatus.SKIPPED, EnumSet.noneOf(QueueEntryStatus.class));
        TRANSITIONS.put(QueueEntryStatus.CANCELLED, EnumSet.noneOf(QueueEntryStatus.class));
        TRANSITIONS.put(QueueEntryStatus.NO_SHOW, EnumSet.noneOf(QueueEntryStatus.class));
    }

    private QueueStateMachine() {
    }

    public static boolean canTransition(QueueEntryStatus from, QueueEntryStatus to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public static void requireTransition(QueueEntryStatus from, QueueEntryStatus to) {
        if (!canTransition(from, to)) {
            throw new InvalidStateTransitionException(
                    "Cannot transition queue entry from " + from + " to " + to + ".");
        }
    }

    /** Statuses considered "concurrently active" for the one-active-entry-per-customer rule. */
    public static Set<QueueEntryStatus> activeStatuses() {
        return EnumSet.of(QueueEntryStatus.WAITING, QueueEntryStatus.CALLED, QueueEntryStatus.SERVING);
    }
}
