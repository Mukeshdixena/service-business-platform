package com.platform.membership;

import com.platform.common.exception.InvalidStateTransitionException;
import com.platform.membership.domain.MembershipStateMachine;
import com.platform.membership.domain.MembershipStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MembershipStateMachineTest {

    @ParameterizedTest
    @CsvSource({
            "PENDING, ACTIVE",
            "PENDING, CANCELLED",
            "ACTIVE, FROZEN",
            "ACTIVE, CANCELLED",
            "ACTIVE, EXPIRED",
            "FROZEN, ACTIVE",
            "FROZEN, CANCELLED"
    })
    void allowsEveryTransitionDefinedInTheContract(MembershipStatus from, MembershipStatus to) {
        assertThat(MembershipStateMachine.canTransition(from, to)).isTrue();
        MembershipStateMachine.requireTransition(from, to); // must not throw
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING, FROZEN",
            "PENDING, EXPIRED",
            "ACTIVE, PENDING",
            "FROZEN, EXPIRED",
            "FROZEN, PENDING",
            "EXPIRED, ACTIVE",
            "CANCELLED, ACTIVE"
    })
    void rejectsEveryTransitionNotDefinedInTheContract(MembershipStatus from, MembershipStatus to) {
        assertThat(MembershipStateMachine.canTransition(from, to)).isFalse();
        assertThatThrownBy(() -> MembershipStateMachine.requireTransition(from, to))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void terminalStatusesHaveNoOutgoingTransitions() {
        for (MembershipStatus terminal : EnumSet.of(MembershipStatus.EXPIRED, MembershipStatus.CANCELLED)) {
            for (MembershipStatus target : MembershipStatus.values()) {
                assertThat(MembershipStateMachine.canTransition(terminal, target)).isFalse();
            }
        }
    }
}
