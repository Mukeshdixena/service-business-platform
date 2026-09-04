package com.platform.membership.controller;

import com.platform.common.pagination.PageResponse;
import com.platform.common.security.CurrentUser;
import com.platform.membership.domain.MembershipStatus;
import com.platform.membership.dto.CreatePurchaseMembershipRequest;
import com.platform.membership.dto.MembershipDto;
import com.platform.membership.service.MembershipService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/memberships")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    /** Customer-facing purchase — any authenticated user acting as a customer, not membership-gated. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public MembershipDto purchase(@PathVariable UUID businessId, @Valid @RequestBody CreatePurchaseMembershipRequest request) {
        return membershipService.purchase(businessId, CurrentUser.id(), request);
    }

    @GetMapping
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public PageResponse<MembershipDto> list(@PathVariable UUID businessId,
                                             @RequestParam(required = false) MembershipStatus status,
                                             Pageable pageable) {
        return membershipService.listForBusiness(businessId, status, pageable);
    }

    @PostMapping("/{id}/freeze")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public MembershipDto freeze(@PathVariable UUID businessId, @PathVariable UUID id) {
        return membershipService.freeze(businessId, id);
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public MembershipDto reactivate(@PathVariable UUID businessId, @PathVariable UUID id) {
        return membershipService.reactivate(businessId, id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public MembershipDto cancel(@PathVariable UUID businessId, @PathVariable UUID id) {
        return membershipService.cancelByBusiness(businessId, id);
    }
}
