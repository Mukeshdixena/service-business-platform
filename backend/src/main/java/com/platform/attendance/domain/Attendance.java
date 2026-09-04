package com.platform.attendance.domain;

import com.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A single visit: the customer walked in ({@code checkInAt}) and possibly left
 * again ({@code checkOutAt}). Attendance is intentionally independent of
 * membership (CLAUDE_CODE.md §18) — holding an ACTIVE membership says nothing
 * about whether the customer is currently on the premises.
 *
 * <p>A row with {@code checkOutAt == null} is "open"; counting open rows is the
 * only source of truth for current occupancy.
 */
@Entity
@Table(name = "attendance")
public class Attendance extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "check_in_at", nullable = false)
    private Instant checkInAt;

    @Column(name = "check_out_at")
    private Instant checkOutAt;

    protected Attendance() {
    }

    public Attendance(UUID businessId, UUID customerId, Instant checkInAt) {
        this.businessId = businessId;
        this.customerId = customerId;
        this.checkInAt = checkInAt;
    }

    public boolean isOpen() {
        return checkOutAt == null;
    }

    public void checkOut(Instant at) {
        this.checkOutAt = at;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public Instant getCheckInAt() {
        return checkInAt;
    }

    public Instant getCheckOutAt() {
        return checkOutAt;
    }
}
