package com.platform.membership.domain;

import com.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "membership_plan")
public class MembershipPlan extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Integer duration;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_unit", nullable = false)
    private MembershipDurationUnit durationUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipPlanStatus status = MembershipPlanStatus.ACTIVE;

    protected MembershipPlan() {
    }

    public MembershipPlan(UUID businessId, String name, String description, BigDecimal price, String currency,
                           Integer duration, MembershipDurationUnit durationUnit) {
        this.businessId = businessId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.duration = duration;
        this.durationUnit = durationUnit;
    }

    public void applyUpdate(String name, String description, BigDecimal price, String currency,
                             Integer duration, MembershipDurationUnit durationUnit, MembershipPlanStatus status) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (price != null) this.price = price;
        if (currency != null) this.currency = currency;
        if (duration != null) this.duration = duration;
        if (durationUnit != null) this.durationUnit = durationUnit;
        if (status != null) this.status = status;
    }

    public void deactivate() {
        this.status = MembershipPlanStatus.INACTIVE;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public Integer getDuration() {
        return duration;
    }

    public MembershipDurationUnit getDurationUnit() {
        return durationUnit;
    }

    public MembershipPlanStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == MembershipPlanStatus.ACTIVE;
    }
}
