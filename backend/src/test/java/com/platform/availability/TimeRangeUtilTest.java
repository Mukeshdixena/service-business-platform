package com.platform.availability;

import com.platform.availability.service.TimeRangeUtil;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TimeRangeUtilTest {

    private final Instant t0 = Instant.parse("2026-09-10T09:00:00Z");
    private final Instant t1 = Instant.parse("2026-09-10T09:30:00Z");
    private final Instant t2 = Instant.parse("2026-09-10T10:00:00Z");
    private final Instant t3 = Instant.parse("2026-09-10T10:30:00Z");

    @Test
    void identicalIntervalsOverlap() {
        assertThat(TimeRangeUtil.overlaps(t0, t2, t0, t2)).isTrue();
    }

    @Test
    void partiallyOverlappingIntervalsOverlap() {
        assertThat(TimeRangeUtil.overlaps(t0, t2, t1, t3)).isTrue();
        assertThat(TimeRangeUtil.overlaps(t1, t3, t0, t2)).isTrue();
    }

    @Test
    void backToBackIntervalsDoNotOverlap() {
        // [t0,t1) and [t1,t3) touch at the boundary but don't overlap — this is
        // what allows back-to-back appointments to be booked with no gap.
        assertThat(TimeRangeUtil.overlaps(t0, t1, t1, t3)).isFalse();
        assertThat(TimeRangeUtil.overlaps(t1, t3, t0, t1)).isFalse();
    }

    @Test
    void disjointIntervalsDoNotOverlap() {
        assertThat(TimeRangeUtil.overlaps(t0, t1, t2, t3)).isFalse();
    }

    @Test
    void oneIntervalFullyContainingAnotherOverlaps() {
        assertThat(TimeRangeUtil.overlaps(t0, t3, t1, t2)).isTrue();
    }
}
