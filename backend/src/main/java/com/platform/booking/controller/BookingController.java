package com.platform.booking.controller;

import com.platform.booking.domain.BookingStatus;
import com.platform.booking.dto.BookingDto;
import com.platform.booking.dto.CancelBookingRequest;
import com.platform.booking.dto.CreateBookingRequest;
import com.platform.booking.service.BookingService;
import com.platform.common.pagination.PageResponse;
import com.platform.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public PageResponse<BookingDto> list(@PathVariable UUID businessId,
                                          @RequestParam(required = false) BookingStatus status,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                          Pageable pageable) {
        return bookingService.listForBusiness(businessId, status, from, to, pageable);
    }

    /** Customer-facing creation — any authenticated user acting as a customer, not membership-gated. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public BookingDto create(@PathVariable UUID businessId, @Valid @RequestBody CreateBookingRequest request) {
        return bookingService.create(businessId, CurrentUser.id(), request);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public BookingDto confirm(@PathVariable UUID businessId, @PathVariable UUID id) {
        return bookingService.confirm(businessId, id);
    }

    @PostMapping("/{id}/check-in")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public BookingDto checkIn(@PathVariable UUID businessId, @PathVariable UUID id) {
        return bookingService.checkIn(businessId, id);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public BookingDto start(@PathVariable UUID businessId, @PathVariable UUID id) {
        return bookingService.start(businessId, id);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public BookingDto complete(@PathVariable UUID businessId, @PathVariable UUID id) {
        return bookingService.complete(businessId, id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public BookingDto cancel(@PathVariable UUID businessId, @PathVariable UUID id,
                              @RequestBody(required = false) CancelBookingRequest request) {
        String reason = request != null ? request.reason() : null;
        return bookingService.cancelByBusiness(businessId, id, reason);
    }

    @PostMapping("/{id}/no-show")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public BookingDto noShow(@PathVariable UUID businessId, @PathVariable UUID id) {
        return bookingService.noShow(businessId, id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public BookingDto reject(@PathVariable UUID businessId, @PathVariable UUID id,
                              @RequestBody(required = false) CancelBookingRequest request) {
        String reason = request != null ? request.reason() : null;
        return bookingService.reject(businessId, id, reason);
    }
}
