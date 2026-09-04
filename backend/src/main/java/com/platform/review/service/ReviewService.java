package com.platform.review.service;

import com.platform.booking.domain.Booking;
import com.platform.booking.domain.BookingStatus;
import com.platform.booking.repository.BookingRepository;
import com.platform.common.exception.NotFoundException;
import com.platform.common.exception.ValidationException;
import com.platform.common.pagination.PageResponse;
import com.platform.customer.domain.CustomerProfile;
import com.platform.customer.service.CustomerProfileService;
import com.platform.review.domain.Review;
import com.platform.review.domain.ReviewStatus;
import com.platform.review.dto.CreateReviewRequest;
import com.platform.review.dto.ReviewDto;
import com.platform.review.repository.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final CustomerProfileService customerProfileService;

    public ReviewService(ReviewRepository reviewRepository,
                         BookingRepository bookingRepository,
                         CustomerProfileService customerProfileService) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.customerProfileService = customerProfileService;
    }

    @Transactional
    public ReviewDto createByCustomer(UUID userId, CreateReviewRequest request) {
        CustomerProfile customer = customerProfileService.getOrCreateEntity(userId);
        Booking booking = bookingRepository.findById(request.bookingId())
                .orElseThrow(() -> NotFoundException.of("Booking", request.bookingId()));

        if (!booking.getCustomerId().equals(customer.getId())) {
            throw new ValidationException("You can only review your own bookings.");
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new ValidationException("You can only review completed bookings.");
        }

        if (reviewRepository.existsByBookingIdAndCustomerProfileId(request.bookingId(), customer.getId())) {
            throw new ValidationException("You have already reviewed this booking.");
        }

        Review review = new Review(
                booking.getBusinessId(),
                customer.getId(),
                request.bookingId(),
                request.rating(),
                request.comment()
        );
        return toDto(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> listForBusiness(UUID businessId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviews;

        if (status != null && !status.isBlank()) {
            ReviewStatus reviewStatus = ReviewStatus.valueOf(status);
            reviews = reviewRepository.findByBusinessIdAndStatusOrderByCreatedAtDesc(businessId, reviewStatus, pageable);
        } else {
            reviews = reviewRepository.findByBusinessIdOrderByCreatedAtDesc(businessId, pageable);
        }

        return toPageResponse(reviews);
    }

    @Transactional
    public ReviewDto approve(UUID businessId, UUID reviewId) {
        Review review = getOwned(businessId, reviewId);
        review.approve();
        return toDto(reviewRepository.save(review));
    }

    @Transactional
    public ReviewDto reject(UUID businessId, UUID reviewId) {
        Review review = getOwned(businessId, reviewId);
        review.reject();
        return toDto(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> listByCustomer(UUID userId, int page, int size) {
        CustomerProfile customer = customerProfileService.getOrCreateEntity(userId);
        Page<Review> reviews = reviewRepository.findByCustomerProfileIdOrderByCreatedAtDesc(
                customer.getId(), PageRequest.of(page, size));
        return toPageResponse(reviews);
    }

    private Review getOwned(UUID businessId, UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> NotFoundException.of("Review", reviewId));
        if (!review.getBusinessId().equals(businessId)) {
            throw new NotFoundException("Review not found: " + reviewId);
        }
        return review;
    }

    public static ReviewDto toDto(Review r) {
        return new ReviewDto(
                r.getId(),
                r.getBusinessId(),
                r.getCustomerProfileId(),
                r.getBookingId(),
                r.getRating(),
                r.getComment(),
                r.getStatus().name(),
                r.getCreatedAt().toString(),
                r.getUpdatedAt().toString()
        );
    }

    private PageResponse<ReviewDto> toPageResponse(Page<Review> page) {
        return new PageResponse<>(
                page.getContent().stream().map(ReviewService::toDto).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
