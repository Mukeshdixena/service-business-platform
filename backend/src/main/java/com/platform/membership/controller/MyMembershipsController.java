package com.platform.membership.controller;

import com.platform.customer.service.CustomerProfileService;
import com.platform.common.security.CurrentUser;
import com.platform.membership.dto.MembershipDto;
import com.platform.membership.service.MembershipService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** The caller can only ever see/act on their own memberships (API_CONTRACT.md: "/me/** "). */
@RestController
@RequestMapping("/api/v1/me/memberships")
@PreAuthorize("isAuthenticated()")
public class MyMembershipsController {

    private final MembershipService membershipService;
    private final CustomerProfileService customerProfileService;

    public MyMembershipsController(MembershipService membershipService, CustomerProfileService customerProfileService) {
        this.membershipService = membershipService;
        this.customerProfileService = customerProfileService;
    }

    @GetMapping
    public List<MembershipDto> myMemberships() {
        UUID customerProfileId = customerProfileService.getOrCreateEntity(CurrentUser.id()).getId();
        return membershipService.listForCustomer(customerProfileId);
    }

    @PostMapping("/{id}/cancel")
    public MembershipDto cancel(@PathVariable UUID id) {
        UUID customerProfileId = customerProfileService.getOrCreateEntity(CurrentUser.id()).getId();
        return membershipService.cancelByCustomer(id, customerProfileId);
    }
}
