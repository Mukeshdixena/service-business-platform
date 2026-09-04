package com.platform.booking.domain;

import com.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking")
public class Booking extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "staff_id")
    private UUID staffId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "cancel_reason")
    private String cancelReason;

    protected Booking() {
    }

    public Booking(UUID businessId, UUID customerId, UUID serviceId, UUID staffId, Instant startAt, Instant endAt,
                    BigDecimal price, String currency, String notes) {
        this.businessId = businessId;
        this.customerId = customerId;
        this.serviceId = serviceId;
        this.staffId = staffId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.price = price;
        this.currency = currency;
        this.notes = notes;
    }

    /** Server-enforced transition — see {@link BookingStateMachine}. Never call setStatus directly. */
    public void transitionTo(BookingStatus target) {
        BookingStateMachine.requireTransition(this.status, target);
        this.status = target;
    }

    public void transitionTo(BookingStatus target, String reason) {
        transitionTo(target);
        if (reason != null) {
            this.cancelReason = reason;
        }
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public UUID getStaffId() {
        return staffId;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public String getNotes() {
        return notes;
    }

    public String getCancelReason() {
        return cancelReason;
    }
}
