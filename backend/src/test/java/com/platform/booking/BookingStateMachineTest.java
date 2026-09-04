package com.platform.booking;

import com.platform.booking.domain.BookingStateMachine;
import com.platform.booking.domain.BookingStatus;
import com.platform.common.exception.InvalidStateTransitionException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingStateMachineTest {

    @ParameterizedTest
    @CsvSource({
            "PENDING, CONFIRMED",
            "PENDING, REJECTED",
            "PENDING, CANCELLED",
            "CONFIRMED, CHECKED_IN",
            "CONFIRMED, CANCELLED",
            "CONFIRMED, NO_SHOW",
            "CHECKED_IN, IN_PROGRESS",
            "CHECKED_IN, CANCELLED",
            "IN_PROGRESS, COMPLETED"
    })
    void allowsEveryTransitionDefinedInTheContract(BookingStatus from, BookingStatus to) {
        assertThat(BookingStateMachine.canTransition(from, to)).isTrue();
        assertThatCode(from, to);
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING, CHECKED_IN",
            "PENDING, IN_PROGRESS",
            "PENDING, COMPLETED",
            "PENDING, NO_SHOW",
            "CONFIRMED, COMPLETED",
            "CONFIRMED, REJECTED",
            "CONFIRMED, IN_PROGRESS",
            "CHECKED_IN, CONFIRMED",
            "CHECKED_IN, NO_SHOW",
            "IN_PROGRESS, CANCELLED",
            "IN_PROGRESS, PENDING",
            "COMPLETED, PENDING",
            "CANCELLED, CONFIRMED",
            "REJECTED, PENDING",
            "NO_SHOW, CONFIRMED"
    })
    void rejectsEveryTransitionNotDefinedInTheContract(BookingStatus from, BookingStatus to) {
        assertThat(BookingStateMachine.canTransition(from, to)).isFalse();
        assertThatThrownBy(() -> BookingStateMachine.requireTransition(from, to))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void terminalStatusesHaveNoOutgoingTransitions() {
        for (BookingStatus terminal : EnumSet.of(BookingStatus.COMPLETED, BookingStatus.CANCELLED,
                BookingStatus.NO_SHOW, BookingStatus.REJECTED)) {
            for (BookingStatus target : BookingStatus.values()) {
                assertThat(BookingStateMachine.canTransition(terminal, target)).isFalse();
            }
        }
    }

    private void assertThatCode(BookingStatus from, BookingStatus to) {
        BookingStateMachine.requireTransition(from, to); // must not throw
    }
}
