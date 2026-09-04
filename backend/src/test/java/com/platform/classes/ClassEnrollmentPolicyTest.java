package com.platform.classes;

import com.platform.classes.domain.ClassEnrollmentPolicy;
import com.platform.classes.domain.ClassEnrollmentStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CLAUDE_CODE.md §20: a full class waitlists the next enrollment rather than
 * rejecting it.
 */
class ClassEnrollmentPolicyTest {

    @Test
    void enrollsWhileThereIsRoom() {
        assertThat(ClassEnrollmentPolicy.statusFor(0, 20)).isEqualTo(ClassEnrollmentStatus.ENROLLED);
        assertThat(ClassEnrollmentPolicy.statusFor(19, 20)).isEqualTo(ClassEnrollmentStatus.ENROLLED);
    }

    @Test
    void waitlistsOnceExactlyFull() {
        assertThat(ClassEnrollmentPolicy.statusFor(20, 20)).isEqualTo(ClassEnrollmentStatus.WAITLISTED);
    }

    @Test
    void waitlistsBeyondCapacityRatherThanRejecting() {
        assertThat(ClassEnrollmentPolicy.statusFor(21, 20)).isEqualTo(ClassEnrollmentStatus.WAITLISTED);
        assertThat(ClassEnrollmentPolicy.statusFor(500, 20)).isEqualTo(ClassEnrollmentStatus.WAITLISTED);
    }

    @Test
    void aSingleSeatClassWaitlistsTheSecondEnrollment() {
        assertThat(ClassEnrollmentPolicy.statusFor(0, 1)).isEqualTo(ClassEnrollmentStatus.ENROLLED);
        assertThat(ClassEnrollmentPolicy.statusFor(1, 1)).isEqualTo(ClassEnrollmentStatus.WAITLISTED);
    }
}
