package com.platform.catalog.domain;

/**
 * The period a RENTAL service's {@code price} is quoted per (CLAUDE_CODE.md §9:
 * "Rental services may be priced by hour / day / custom period"). Only HOUR and
 * DAY exist this phase; the enum is the extension point for custom periods.
 */
public enum PricingUnit {
    HOUR(60),
    DAY(24 * 60);

    private final int minutes;

    PricingUnit(int minutes) {
        this.minutes = minutes;
    }

    public int minutes() {
        return minutes;
    }
}
