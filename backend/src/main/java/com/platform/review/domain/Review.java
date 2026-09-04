package com.platform.review.domain;

import com.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "review")
public class Review extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "customer_profile_id", nullable = false)
    private UUID customerProfileId;

    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    @Column(nullable = false)
    private int rating;

    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status;

    protected Review() {
    }

    public Review(UUID businessId, UUID customerProfileId, UUID bookingId, int rating, String comment) {
        this.businessId = businessId;
        this.customerProfileId = customerProfileId;
        this.bookingId = bookingId;
        this.rating = rating;
        this.comment = comment;
        this.status = ReviewStatus.PENDING;
    }

    public UUID getBusinessId() { return businessId; }
    public UUID getCustomerProfileId() { return customerProfileId; }
    public UUID getBookingId() { return bookingId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public ReviewStatus getStatus() { return status; }

    public void setRating(int rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment; }

    public void approve() {
        this.status = ReviewStatus.APPROVED;
    }

    public void reject() {
        this.status = ReviewStatus.REJECTED;
    }
}
