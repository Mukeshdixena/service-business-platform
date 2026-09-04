package com.platform.booking.controller;

import com.platform.booking.domain.BookingStatus;
import com.platform.booking.dto.BookingDto;
import com.platform.booking.dto.CancelBookingRequest;
import com.platform.booking.service.BookingService;
import com.platform.common.pagination.PageResponse;
import com.platform.common.security.CurrentUser;
import com.platform.customer.service.CustomerProfileService;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/** The caller can only ever see/act on their own bookings (API_CONTRACT.md: "/me/** "). */
@RestController
@RequestMapping("/api/v1/me/bookings")
@PreAuthorize("isAuthenticated()")
public class MyBookingsController {

    private final BookingService bookingService;
    private final CustomerProfileService customerProfileService;

    public MyBookingsController(BookingService bookingService, CustomerProfileService customerProfileService) {
        this.bookingService = bookingService;
        this.customerProfileService = customerProfileService;
    }

    @GetMapping
    public PageResponse<BookingDto> myBookings(@RequestParam(required = false) BookingStatus status,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                                Pageable pageable) {
        UUID customerProfileId = customerProfileService.getOrCreateEntity(CurrentUser.id()).getId();
        return bookingService.listForCustomer(customerProfileId, status, from, to, pageable);
    }

    @PostMapping("/{id}/cancel")
    public BookingDto cancel(@PathVariable UUID id, @RequestBody(required = false) CancelBookingRequest request) {
        UUID customerProfileId = customerProfileService.getOrCreateEntity(CurrentUser.id()).getId();
        String reason = request != null ? request.reason() : null;
        return bookingService.cancelByCustomer(id, customerProfileId, reason);
    }
}
