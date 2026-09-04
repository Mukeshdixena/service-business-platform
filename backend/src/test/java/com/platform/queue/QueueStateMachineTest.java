package com.platform.queue;

import com.platform.common.exception.InvalidStateTransitionException;
import com.platform.queue.domain.QueueEntryStatus;
import com.platform.queue.domain.QueueStateMachine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueueStateMachineTest {

    @ParameterizedTest
    @CsvSource({
            "WAITING, CALLED",
            "WAITING, SKIPPED",
            "WAITING, CANCELLED",
            "CALLED, SERVING",
            "CALLED, NO_SHOW",
            "CALLED, CANCELLED",
            "SERVING, COMPLETED"
    })
    void allowsEveryTransitionDefinedInTheContract(QueueEntryStatus from, QueueEntryStatus to) {
        assertThat(QueueStateMachine.canTransition(from, to)).isTrue();
        QueueStateMachine.requireTransition(from, to); // must not throw
    }

    @ParameterizedTest
    @CsvSource({
            "WAITING, SERVING",
            "WAITING, COMPLETED",
            "WAITING, NO_SHOW",
            "CALLED, SKIPPED",
            "CALLED, COMPLETED",
            "SERVING, CALLED",
            "SERVING, CANCELLED",
            "SERVING, SKIPPED",
            "COMPLETED, WAITING",
            "SKIPPED, WAITING",
            "CANCELLED, WAITING",
            "NO_SHOW, CALLED"
    })
    void rejectsEveryTransitionNotDefinedInTheContract(QueueEntryStatus from, QueueEntryStatus to) {
        assertThat(QueueStateMachine.canTransition(from, to)).isFalse();
        assertThatThrownBy(() -> QueueStateMachine.requireTransition(from, to))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void terminalStatusesHaveNoOutgoingTransitions() {
        for (QueueEntryStatus terminal : EnumSet.of(QueueEntryStatus.COMPLETED, QueueEntryStatus.SKIPPED,
                QueueEntryStatus.CANCELLED, QueueEntryStatus.NO_SHOW)) {
            for (QueueEntryStatus target : QueueEntryStatus.values()) {
                assertThat(QueueStateMachine.canTransition(terminal, target)).isFalse();
            }
        }
    }

    @Test
    void activeStatusesAreWaitingCalledServing() {
        assertThat(QueueStateMachine.activeStatuses())
                .containsExactlyInAnyOrder(QueueEntryStatus.WAITING, QueueEntryStatus.CALLED, QueueEntryStatus.SERVING);
    }
}
