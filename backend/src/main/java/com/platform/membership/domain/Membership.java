package com.platform.membership.domain;

import com.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/** A customer's purchased/assigned membership (CLAUDE_CODE.md §17). */
@Entity
@Table(name = "membership")
public class Membership extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "membership_plan_id", nullable = false)
    private UUID membershipPlanId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status = MembershipStatus.PENDING;

    /** Always null this phase — no real payment provider is wired up yet (CLAUDE_CODE.md §22). */
    @Column(name = "payment_id")
    private UUID paymentId;

    protected Membership() {
    }

    public Membership(UUID businessId, UUID customerId, UUID membershipPlanId, LocalDate startDate, LocalDate endDate) {
        this.businessId = businessId;
        this.customerId = customerId;
        this.membershipPlanId = membershipPlanId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /** Server-enforced transition — see {@link MembershipStateMachine}. Never call setStatus directly. */
    public void transitionTo(MembershipStatus target) {
        MembershipStateMachine.requireTransition(this.status, target);
        this.status = target;
    }

    /** Bypasses the state machine only for the one system-driven transition: ACTIVE -> EXPIRED once endDate has passed. */
    public void markExpired() {
        this.status = MembershipStatus.EXPIRED;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getMembershipPlanId() {
        return membershipPlanId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public boolean isEffectivelyExpired(LocalDate today) {
        return status == MembershipStatus.ACTIVE && endDate.isBefore(today);
    }
}
