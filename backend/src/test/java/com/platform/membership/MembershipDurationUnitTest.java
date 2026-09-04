package com.platform.membership;

import com.platform.membership.domain.MembershipDurationUnit;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** endDate computation for each durationUnit (CLAUDE_CODE.md §17, API_CONTRACT.md purchase behavior). */
class MembershipDurationUnitTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 15);

    @Test
    void day() {
        assertThat(START.plus(MembershipDurationUnit.DAY.toPeriod(10))).isEqualTo(LocalDate.of(2026, 1, 25));
    }

    @Test
    void week() {
        assertThat(START.plus(MembershipDurationUnit.WEEK.toPeriod(2))).isEqualTo(LocalDate.of(2026, 1, 29));
    }

    @Test
    void month() {
        assertThat(START.plus(MembershipDurationUnit.MONTH.toPeriod(1))).isEqualTo(LocalDate.of(2026, 2, 15));
    }

    @Test
    void year() {
        assertThat(START.plus(MembershipDurationUnit.YEAR.toPeriod(1))).isEqualTo(LocalDate.of(2027, 1, 15));
    }
}
