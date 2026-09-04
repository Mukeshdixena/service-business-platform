package com.platform.staff.controller;

import com.platform.common.pagination.PageResponse;
import com.platform.staff.dto.CreateStaffRequest;
import com.platform.staff.dto.StaffDto;
import com.platform.staff.dto.UpdateStaffRequest;
import com.platform.staff.service.StaffMemberService;
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
@RequestMapping("/api/v1/businesses/{businessId}/staff")
public class StaffController {

    private final StaffMemberService staffMemberService;

    public StaffController(StaffMemberService staffMemberService) {
        this.staffMemberService = staffMemberService;
    }

    @GetMapping
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public PageResponse<StaffDto> list(@PathVariable UUID businessId, Pageable pageable) {
        return staffMemberService.list(businessId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public StaffDto create(@PathVariable UUID businessId, @Valid @RequestBody CreateStaffRequest request) {
        return staffMemberService.create(businessId, request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public StaffDto update(@PathVariable UUID businessId, @PathVariable UUID id,
                            @Valid @RequestBody UpdateStaffRequest request) {
        return staffMemberService.update(businessId, id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public ResponseEntity<Void> delete(@PathVariable UUID businessId, @PathVariable UUID id) {
        staffMemberService.softDelete(businessId, id);
        return ResponseEntity.noContent().build();
    }
}
