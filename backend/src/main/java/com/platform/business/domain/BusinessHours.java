package com.platform.business.domain;

import com.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/**
 * One open/close interval for a single day of week. Multiple rows per
 * {@code dayOfWeek} are allowed so a business can model e.g. a lunch closure
 * (06:00-10:00, 16:00-22:00) — see CLAUDE_CODE.md §12.
 */
@Entity
@Table(name = "business_hours")
public class BusinessHours extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;

    protected BusinessHours() {
    }

    public BusinessHours(UUID businessId, DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime) {
        this.businessId = businessId;
        this.dayOfWeek = dayOfWeek;
        this.openTime = openTime;
        this.closeTime = closeTime;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getOpenTime() {
        return openTime;
    }

    public LocalTime getCloseTime() {
        return closeTime;
    }
}
