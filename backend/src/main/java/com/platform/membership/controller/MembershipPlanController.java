package com.platform.membership.controller;

import com.platform.common.pagination.PageResponse;
import com.platform.membership.dto.CreateMembershipPlanRequest;
import com.platform.membership.dto.MembershipPlanDto;
import com.platform.membership.dto.UpdateMembershipPlanRequest;
import com.platform.membership.service.MembershipPlanService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/membership-plans")
public class MembershipPlanController {

    private final MembershipPlanService membershipPlanService;

    public MembershipPlanController(MembershipPlanService membershipPlanService) {
        this.membershipPlanService = membershipPlanService;
    }

    @GetMapping
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public PageResponse<MembershipPlanDto> list(@PathVariable UUID businessId, Pageable pageable) {
        return membershipPlanService.list(businessId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public MembershipPlanDto create(@PathVariable UUID businessId, @Valid @RequestBody CreateMembershipPlanRequest request) {
        return membershipPlanService.create(businessId, request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public MembershipPlanDto update(@PathVariable UUID businessId, @PathVariable UUID id,
                                     @Valid @RequestBody UpdateMembershipPlanRequest request) {
        return membershipPlanService.update(businessId, id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public ResponseEntity<Void> delete(@PathVariable UUID businessId, @PathVariable UUID id) {
        membershipPlanService.softDelete(businessId, id);
        return ResponseEntity.noContent().build();
    }
}
