package com.platform.catalog.domain;

import com.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The bookable-item entity (CLAUDE_CODE.md §9). Named {@code Service} per the
 * domain spec; lives in the {@code catalog} package specifically so it never
 * collides with {@code org.springframework.stereotype.Service}.
 */
@Entity
@Table(name = "service")
public class Service extends BaseEntity {

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

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_type", nullable = false)
    private ServiceBookingType bookingType;

    /**
     * The period {@code price} is quoted per for RENTAL services (per-hour or
     * per-day). Null for every other booking type; required by
     * {@code CatalogService} when bookingType = RENTAL.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_unit", length = 10)
    private PricingUnit pricingUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceStatus status = ServiceStatus.ACTIVE;

    protected Service() {
    }

    public Service(UUID businessId, String name, String description, BigDecimal price, String currency,
                    Integer durationMinutes, ServiceBookingType bookingType, PricingUnit pricingUnit) {
        this.businessId = businessId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.durationMinutes = durationMinutes;
        this.bookingType = bookingType;
        this.pricingUnit = pricingUnit;
    }

    public void applyUpdate(String name, String description, BigDecimal price, String currency,
                             Integer durationMinutes, ServiceBookingType bookingType, PricingUnit pricingUnit,
                             ServiceStatus status) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (price != null) this.price = price;
        if (currency != null) this.currency = currency;
        if (durationMinutes != null) this.durationMinutes = durationMinutes;
        if (bookingType != null) this.bookingType = bookingType;
        if (pricingUnit != null) this.pricingUnit = pricingUnit;
        if (status != null) this.status = status;
    }

    public void deactivate() {
        this.status = ServiceStatus.INACTIVE;
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

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public ServiceBookingType getBookingType() {
        return bookingType;
    }

    public PricingUnit getPricingUnit() {
        return pricingUnit;
    }

    public ServiceStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == ServiceStatus.ACTIVE;
    }
}
