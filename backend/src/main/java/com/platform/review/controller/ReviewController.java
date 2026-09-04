package com.platform.review.controller;

import com.platform.common.pagination.PageResponse;
import com.platform.common.security.CurrentUser;
import com.platform.review.dto.ReviewDto;
import com.platform.review.service.ReviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public PageResponse<ReviewDto> list(
            @PathVariable UUID businessId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reviewService.listForBusiness(businessId, status, page, size);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public ReviewDto approve(@PathVariable UUID businessId, @PathVariable UUID id) {
        return reviewService.approve(businessId, id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public ReviewDto reject(@PathVariable UUID businessId, @PathVariable UUID id) {
        return reviewService.reject(businessId, id);
    }
}
