package com.platform.availability.service;

import com.platform.availability.dto.SlotDto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pure slot-generation math — deliberately free of Spring/JPA dependencies so
 * it is trivially unit-testable. Business hours are treated as UTC in this
 * phase (no per-business timezone field yet — see ARCHITECTURE.md).
 *
 * <p>Slots are generated back-to-back within each open interval, each exactly
 * {@code durationMinutes} long, and marked unavailable if they overlap an
 * existing non-terminal booking or start in the past.
 */
public final class SlotCalculator {

    private SlotCalculator() {
    }

    public record OpenInterval(LocalTime openTime, LocalTime closeTime) {
    }

    public record BookedInterval(Instant start, Instant end) {
    }

    public static List<SlotDto> generate(LocalDate date,
                                          List<OpenInterval> openIntervals,
                                          int durationMinutes,
                                          List<BookedInterval> existingBookings,
                                          Instant now,
                                          UUID staffId) {
        List<SlotDto> slots = new ArrayList<>();
        for (OpenInterval interval : openIntervals) {
            Instant intervalStart = date.atTime(interval.openTime()).toInstant(ZoneOffset.UTC);
            Instant intervalEnd = date.atTime(interval.closeTime()).toInstant(ZoneOffset.UTC);
            slots.addAll(generateWindow(intervalStart, intervalEnd, durationMinutes, existingBookings, now, staffId));
        }
        return slots;
    }

    /**
     * Slot generation over an explicit instant window. Used directly for rentals,
     * whose window is a whole calendar day rather than a business-hours interval
     * (a rental may run overnight), and by {@link #generate} for each open
     * interval of an appointment day.
     */
    public static List<SlotDto> generateWindow(Instant windowStart,
                                                Instant windowEnd,
                                                int durationMinutes,
                                                List<BookedInterval> existingBookings,
                                                Instant now,
                                                UUID trackId) {
        List<SlotDto> slots = new ArrayList<>();
        Instant slotStart = windowStart;
        while (true) {
            Instant slotEnd = slotStart.plusSeconds(durationMinutes * 60L);
            if (slotEnd.isAfter(windowEnd)) {
                break;
            }
            Instant fixedSlotStart = slotStart;
            Instant fixedSlotEnd = slotEnd;
            boolean inPast = fixedSlotStart.isBefore(now);
            boolean conflicts = existingBookings.stream()
                    .anyMatch(b -> TimeRangeUtil.overlaps(fixedSlotStart, fixedSlotEnd, b.start(), b.end()));
            slots.add(new SlotDto(fixedSlotStart, fixedSlotEnd, !inPast && !conflicts, trackId));
            slotStart = slotEnd;
        }
        return slots;
    }
}
