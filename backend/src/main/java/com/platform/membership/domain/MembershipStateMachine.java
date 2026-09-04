package com.platform.membership.domain;

import com.platform.common.exception.InvalidStateTransitionException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The explicit, server-enforced membership lifecycle from API_CONTRACT.md /
 * CLAUDE_CODE.md §17. Mirrors {@link com.platform.booking.domain.BookingStateMachine}'s
 * structure. Purchase goes straight to ACTIVE this phase, so PENDING is
 * currently unreachable via any application code path — it is still modeled
 * here for forward-compatibility with Phase 8's real payment flow.
 *
 * <pre>
 * PENDING -&gt; ACTIVE, CANCELLED
 * ACTIVE  -&gt; FROZEN, CANCELLED, EXPIRED
 * FROZEN  -&gt; ACTIVE, CANCELLED
 * </pre>
 *
 * EXPIRED is also reached automatically (not via a client action) once
 * endDate has passed — see {@code MembershipService#effectiveStatus}, which
 * handles that lazily on read rather than via this machine or a scheduled job.
 */
public final class MembershipStateMachine {

    private static final Map<MembershipStatus, Set<MembershipStatus>> TRANSITIONS = new EnumMap<>(MembershipStatus.class);

    static {
        TRANSITIONS.put(MembershipStatus.PENDING,
                EnumSet.of(MembershipStatus.ACTIVE, MembershipStatus.CANCELLED));
        TRANSITIONS.put(MembershipStatus.ACTIVE,
                EnumSet.of(MembershipStatus.FROZEN, MembershipStatus.CANCELLED, MembershipStatus.EXPIRED));
        TRANSITIONS.put(MembershipStatus.FROZEN,
                EnumSet.of(MembershipStatus.ACTIVE, MembershipStatus.CANCELLED));
        TRANSITIONS.put(MembershipStatus.EXPIRED, EnumSet.noneOf(MembershipStatus.class));
        TRANSITIONS.put(MembershipStatus.CANCELLED, EnumSet.noneOf(MembershipStatus.class));
    }

    private MembershipStateMachine() {
    }

    public static boolean canTransition(MembershipStatus from, MembershipStatus to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public static void requireTransition(MembershipStatus from, MembershipStatus to) {
        if (!canTransition(from, to)) {
            throw new InvalidStateTransitionException(
                    "Cannot transition membership from " + from + " to " + to + ".");
        }
    }
}
