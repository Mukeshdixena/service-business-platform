package com.platform.booking;

import com.platform.booking.domain.RentalPricing;
import com.platform.catalog.domain.PricingUnit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API_CONTRACT.md: rental total = per-unit rate x ceil(duration / unit).
 * Ceiling, never rounding — a part-used unit is a charged unit.
 */
class RentalPricingTest {

    private static final Instant START = Instant.parse("2026-09-10T09:00:00Z");
    private static final BigDecimal RATE = new BigDecimal("100.00");

    private BigDecimal priceFor(PricingUnit unit, Duration duration) {
        return RentalPricing.totalPrice(RATE, unit, START, START.plus(duration));
    }

    @Test
    void hourlyRentalOfExactlyOneHourChargesOneUnit() {
        assertThat(priceFor(PricingUnit.HOUR, Duration.ofHours(1))).isEqualByComparingTo("100.00");
    }

    @Test
    void hourlyRentalOfThreeHoursChargesThreeUnits() {
        assertThat(priceFor(PricingUnit.HOUR, Duration.ofHours(3))).isEqualByComparingTo("300.00");
    }

    @Test
    void hourlyRentalRoundsPartHoursUp() {
        assertThat(priceFor(PricingUnit.HOUR, Duration.ofMinutes(90))).isEqualByComparingTo("200.00");
        assertThat(priceFor(PricingUnit.HOUR, Duration.ofMinutes(61))).isEqualByComparingTo("200.00");
        assertThat(priceFor(PricingUnit.HOUR, Duration.ofMinutes(15))).isEqualByComparingTo("100.00");
    }

    @Test
    void dailyRentalOfExactlyOneDayChargesOneUnit() {
        assertThat(priceFor(PricingUnit.DAY, Duration.ofDays(1))).isEqualByComparingTo("100.00");
    }

    @Test
    void dailyRentalRoundsPartDaysUp() {
        assertThat(priceFor(PricingUnit.DAY, Duration.ofHours(25))).isEqualByComparingTo("200.00");
        assertThat(priceFor(PricingUnit.DAY, Duration.ofHours(2))).isEqualByComparingTo("100.00");
        assertThat(priceFor(PricingUnit.DAY, Duration.ofDays(3))).isEqualByComparingTo("300.00");
        assertThat(priceFor(PricingUnit.DAY, Duration.ofDays(3).plusMinutes(1))).isEqualByComparingTo("400.00");
    }

    @Test
    void unitCountIsAlwaysAtLeastOne() {
        assertThat(RentalPricing.unitsFor(0, PricingUnit.HOUR)).isEqualTo(1);
        assertThat(RentalPricing.unitsFor(1, PricingUnit.DAY)).isEqualTo(1);
    }

    @Test
    void aFractionalRateIsMultipliedExactly() {
        BigDecimal price = RentalPricing.totalPrice(new BigDecimal("12.50"), PricingUnit.HOUR, START,
                START.plus(Duration.ofMinutes(150)));
        assertThat(price).isEqualByComparingTo("37.50");
    }
}
