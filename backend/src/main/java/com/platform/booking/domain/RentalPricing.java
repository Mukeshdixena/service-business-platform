package com.platform.booking.domain;

import com.platform.catalog.domain.PricingUnit;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/**
 * Rental total = per-unit rate x ceil(duration / unit) (API_CONTRACT.md
 * ServiceDto.pricingUnit). Deliberately pure/static so the rounding behaviour is
 * unit-testable without a database.
 *
 * <p>Ceiling, not rounding: a 90-minute hourly rental is charged 2 hours, and a
 * 25-hour daily rental is charged 2 days. A rental is always charged for at
 * least one whole unit.
 */
public final class RentalPricing {

    private RentalPricing() {
    }

    public static BigDecimal totalPrice(BigDecimal perUnitPrice, PricingUnit unit, Instant startAt, Instant endAt) {
        long minutes = Duration.between(startAt, endAt).toMinutes();
        return perUnitPrice.multiply(BigDecimal.valueOf(unitsFor(minutes, unit)));
    }

    /** Number of chargeable whole units covering {@code durationMinutes}; never less than 1. */
    public static long unitsFor(long durationMinutes, PricingUnit unit) {
        if (durationMinutes <= 0) {
            return 1;
        }
        long unitMinutes = unit.minutes();
        return (durationMinutes + unitMinutes - 1) / unitMinutes;
    }
}
