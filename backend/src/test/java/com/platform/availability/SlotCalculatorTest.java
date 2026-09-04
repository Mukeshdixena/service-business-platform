package com.platform.availability;

import com.platform.availability.dto.SlotDto;
import com.platform.availability.service.SlotCalculator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SlotCalculatorTest {

    private final LocalDate date = LocalDate.of(2026, 9, 10); // a Thursday
    private final UUID staffId = UUID.randomUUID();

    @Test
    void generatesBackToBackSlotsFillingTheWholeInterval() {
        List<SlotCalculator.OpenInterval> hours = List.of(new SlotCalculator.OpenInterval(
                LocalTime.of(9, 0), LocalTime.of(10, 0)));
        Instant now = Instant.parse("2026-09-01T00:00:00Z");

        List<SlotDto> slots = SlotCalculator.generate(date, hours, 30, List.of(), now, staffId);

        assertThat(slots).hasSize(2);
        assertThat(slots.get(0).start()).isEqualTo(Instant.parse("2026-09-10T09:00:00Z"));
        assertThat(slots.get(0).end()).isEqualTo(Instant.parse("2026-09-10T09:30:00Z"));
        assertThat(slots.get(1).start()).isEqualTo(Instant.parse("2026-09-10T09:30:00Z"));
        assertThat(slots.get(1).end()).isEqualTo(Instant.parse("2026-09-10T10:00:00Z"));
        assertThat(slots).allMatch(SlotDto::available);
    }

    @Test
    void doesNotGenerateAPartialSlotThatWouldOverrunClosingTime() {
        // 09:00-09:50 with a 30-minute service: only one full slot fits (09:00-09:30);
        // 09:30-10:00 would overrun the 09:50 close and must not be produced.
        List<SlotCalculator.OpenInterval> hours = List.of(new SlotCalculator.OpenInterval(
                LocalTime.of(9, 0), LocalTime.of(9, 50)));
        Instant now = Instant.parse("2026-09-01T00:00:00Z");

        List<SlotDto> slots = SlotCalculator.generate(date, hours, 30, List.of(), now, staffId);

        assertThat(slots).hasSize(1);
        assertThat(slots.get(0).start()).isEqualTo(Instant.parse("2026-09-10T09:00:00Z"));
    }

    @Test
    void marksSlotsOverlappingAnExistingBookingAsUnavailable() {
        List<SlotCalculator.OpenInterval> hours = List.of(new SlotCalculator.OpenInterval(
                LocalTime.of(9, 0), LocalTime.of(10, 0)));
        Instant now = Instant.parse("2026-09-01T00:00:00Z");
        List<SlotCalculator.BookedInterval> booked = List.of(new SlotCalculator.BookedInterval(
                Instant.parse("2026-09-10T09:00:00Z"), Instant.parse("2026-09-10T09:30:00Z")));

        List<SlotDto> slots = SlotCalculator.generate(date, hours, 30, booked, now, staffId);

        assertThat(slots).hasSize(2);
        assertThat(slots.get(0).available()).isFalse();
        assertThat(slots.get(1).available()).isTrue();
    }

    @Test
    void marksPastSlotsAsUnavailableEvenWithoutABooking() {
        List<SlotCalculator.OpenInterval> hours = List.of(new SlotCalculator.OpenInterval(
                LocalTime.of(9, 0), LocalTime.of(10, 0)));
        // "now" is after the first slot's start but before the second's.
        Instant now = Instant.parse("2026-09-10T09:15:00Z");

        List<SlotDto> slots = SlotCalculator.generate(date, hours, 30, List.of(), now, staffId);

        assertThat(slots.get(0).available()).isFalse();
        assertThat(slots.get(1).available()).isTrue();
    }

    @Test
    void multipleIntervalsInADayEachProduceTheirOwnSlots() {
        // e.g. a business open 06:00-10:00 and again 16:00-22:00 (CLAUDE_CODE.md §12).
        List<SlotCalculator.OpenInterval> hours = List.of(
                new SlotCalculator.OpenInterval(LocalTime.of(6, 0), LocalTime.of(7, 0)),
                new SlotCalculator.OpenInterval(LocalTime.of(16, 0), LocalTime.of(17, 0)));
        Instant now = Instant.parse("2026-09-01T00:00:00Z");

        List<SlotDto> slots = SlotCalculator.generate(date, hours, 60, List.of(), now, staffId);

        assertThat(slots).hasSize(2);
        assertThat(slots.get(0).start()).isEqualTo(Instant.parse("2026-09-10T06:00:00Z"));
        assertThat(slots.get(1).start()).isEqualTo(Instant.parse("2026-09-10T16:00:00Z"));
    }

    @Test
    void noOpenIntervalsProducesNoSlots() {
        Instant now = Instant.parse("2026-09-01T00:00:00Z");
        List<SlotDto> slots = SlotCalculator.generate(date, List.of(), 30, List.of(), now, staffId);
        assertThat(slots).isEmpty();
    }
}
