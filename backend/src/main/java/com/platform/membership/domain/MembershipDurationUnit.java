package com.platform.membership.domain;

import java.time.Period;

public enum MembershipDurationUnit {
    DAY,
    WEEK,
    MONTH,
    YEAR;

    /** The java.time.Period corresponding to {@code amount} units of this type. */
    public Period toPeriod(int amount) {
        return switch (this) {
            case DAY -> Period.ofDays(amount);
            case WEEK -> Period.ofWeeks(amount);
            case MONTH -> Period.ofMonths(amount);
            case YEAR -> Period.ofYears(amount);
        };
    }
}
