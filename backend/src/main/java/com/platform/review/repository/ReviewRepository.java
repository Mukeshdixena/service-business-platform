package com.platform.review.repository;

import com.platform.review.domain.Review;
import com.platform.review.domain.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByBookingId(UUID bookingId);

    boolean existsByBookingIdAndCustomerProfileId(UUID bookingId, UUID customerProfileId);

    Page<Review> findByBusinessIdAndStatusOrderByCreatedAtDesc(UUID businessId, ReviewStatus status, Pageable pageable);

    Page<Review> findByBusinessIdOrderByCreatedAtDesc(UUID businessId, Pageable pageable);

    Page<Review> findByCustomerProfileIdOrderByCreatedAtDesc(UUID customerProfileId, Pageable pageable);

    long countByBusinessIdAndStatus(UUID businessId, ReviewStatus status);

    long countByStatus(ReviewStatus status);
}
