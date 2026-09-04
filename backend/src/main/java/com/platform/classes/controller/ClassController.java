package com.platform.classes.controller;

import com.platform.classes.domain.ClassStatus;
import com.platform.classes.dto.ClassDto;
import com.platform.classes.dto.ClassEnrollmentDto;
import com.platform.classes.dto.CreateClassRequest;
import com.platform.classes.dto.UpdateClassRequest;
import com.platform.classes.service.ClassService;
import com.platform.common.pagination.PageResponse;
import com.platform.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/classes")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    @GetMapping
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public PageResponse<ClassDto> list(@PathVariable UUID businessId,
                                        @RequestParam(required = false) ClassStatus status,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                        Pageable pageable) {
        return classService.list(businessId, status, from, to, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public ClassDto create(@PathVariable UUID businessId, @Valid @RequestBody CreateClassRequest request) {
        return classService.create(businessId, request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public ClassDto update(@PathVariable UUID businessId, @PathVariable UUID id,
                            @Valid @RequestBody UpdateClassRequest request) {
        return classService.update(businessId, id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'OWNER')")
    public ResponseEntity<Void> delete(@PathVariable UUID businessId, @PathVariable UUID id) {
        classService.softDelete(businessId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/enrollments")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public List<ClassEnrollmentDto> roster(@PathVariable UUID businessId, @PathVariable UUID id) {
        return classService.roster(businessId, id);
    }

    @PostMapping("/{id}/enrollments/{enrollmentId}/attended")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public ClassEnrollmentDto attended(@PathVariable UUID businessId, @PathVariable UUID id,
                                        @PathVariable UUID enrollmentId) {
        return classService.markAttended(businessId, id, enrollmentId);
    }

    @PostMapping("/{id}/enrollments/{enrollmentId}/no-show")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public ClassEnrollmentDto noShow(@PathVariable UUID businessId, @PathVariable UUID id,
                                      @PathVariable UUID enrollmentId) {
        return classService.markNoShow(businessId, id, enrollmentId);
    }

    /** Customer-facing enroll — any authenticated user acting as a customer, not membership-gated. */
    @PostMapping("/{id}/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public ClassEnrollmentDto enroll(@PathVariable UUID businessId, @PathVariable UUID id) {
        return classService.enroll(businessId, id, CurrentUser.id());
    }

    /** Customer cancels their own enrollment only — scoped by the caller's customer profile. */
    @PostMapping("/{id}/cancel-enrollment")
    @PreAuthorize("isAuthenticated()")
    public ClassEnrollmentDto cancelEnrollment(@PathVariable UUID businessId, @PathVariable UUID id) {
        return classService.cancelEnrollment(businessId, id, CurrentUser.id());
    }
}
