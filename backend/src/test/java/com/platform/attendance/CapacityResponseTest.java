package com.platform.attendance;

import com.platform.attendance.dto.CapacityResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** CLAUDE_CODE.md §19: available = capacity - current, derived, never stored. */
class CapacityResponseTest {

    @Test
    void computesAvailableFromCurrentAndCapacity() {
        CapacityResponse response = CapacityResponse.of(87, 150);

        assertThat(response.current()).isEqualTo(87);
        assertThat(response.capacity()).isEqualTo(150);
        assertThat(response.available()).isEqualTo(63);
    }

    @Test
    void anEmptyVenueHasFullAvailability() {
        assertThat(CapacityResponse.of(0, 20).available()).isEqualTo(20);
    }

    @Test
    void aFullVenueHasZeroAvailable() {
        assertThat(CapacityResponse.of(20, 20).available()).isZero();
    }

    @Test
    void neverReportsNegativeAvailabilityWhenOverCapacity() {
        CapacityResponse response = CapacityResponse.of(25, 20);

        assertThat(response.current()).isEqualTo(25);
        assertThat(response.available()).isZero();
    }

    @Test
    void unconfiguredCapacityReportsNullCapacityAndAvailableNotZero() {
        CapacityResponse response = CapacityResponse.of(12, null);

        assertThat(response.current()).isEqualTo(12);
        assertThat(response.capacity()).isNull();
        assertThat(response.available()).isNull();
    }
}
