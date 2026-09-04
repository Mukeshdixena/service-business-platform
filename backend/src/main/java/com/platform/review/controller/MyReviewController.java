package com.platform.review.controller;

import com.platform.common.pagination.PageResponse;
import com.platform.common.security.CurrentUser;
import com.platform.review.dto.CreateReviewRequest;
import com.platform.review.dto.ReviewDto;
import com.platform.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/reviews")
public class MyReviewController {

    private final ReviewService reviewService;

    public MyReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto create(@Valid @RequestBody CreateReviewRequest request) {
        return reviewService.createByCustomer(CurrentUser.id(), request);
    }

    @GetMapping
    public PageResponse<ReviewDto> listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reviewService.listByCustomer(CurrentUser.id(), page, size);
    }
}
