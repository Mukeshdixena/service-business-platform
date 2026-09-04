package com.platform.attendance.controller;

import com.platform.attendance.dto.AttendanceDto;
import com.platform.attendance.dto.CreateAttendanceRequest;
import com.platform.attendance.service.AttendanceService;
import com.platform.common.pagination.PageResponse;
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

/** Front-desk operations — OWNER/STAFF only; customers never check themselves in. */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public AttendanceDto checkIn(@PathVariable UUID businessId, @Valid @RequestBody CreateAttendanceRequest request) {
        return attendanceService.checkIn(businessId, request);
    }

    @PostMapping("/{id}/check-out")
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public AttendanceDto checkOut(@PathVariable UUID businessId, @PathVariable UUID id) {
        return attendanceService.checkOut(businessId, id);
    }

    @GetMapping
    @PreAuthorize("@businessAccessService.hasRole(#businessId, authentication, 'STAFF')")
    public PageResponse<AttendanceDto> list(@PathVariable UUID businessId,
                                             @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
                                             Pageable pageable) {
        return attendanceService.list(businessId, activeOnly, pageable);
    }
}
