package com.platform.attendance.service;

import com.platform.attendance.domain.Attendance;
import com.platform.attendance.dto.AttendanceDto;
import com.platform.attendance.dto.CapacityResponse;
import com.platform.attendance.dto.CreateAttendanceRequest;
import com.platform.attendance.repository.AttendanceRepository;
import com.platform.business.domain.Business;
import com.platform.business.service.BusinessService;
import com.platform.common.exception.AttendanceConflictException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.pagination.PageResponse;
import com.platform.customer.repository.CustomerProfileRepository;
import com.platform.customer.service.CustomerProfileService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Attendance and derived capacity (CLAUDE_CODE.md §18-19).
 *
 * <p>Occupancy is never stored. {@link #capacity(UUID)} issues a live
 * {@code COUNT(*)} over attendance rows with {@code checkOutAt IS NULL}, so it
 * can never drift out of sync with the underlying check-in/check-out events —
 * which is exactly the failure mode §18 warns against.
 */
@org.springframework.stereotype.Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final BusinessService businessService;
    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerProfileService customerProfileService;

    @PersistenceContext
    private EntityManager entityManager;

    public AttendanceService(AttendanceRepository attendanceRepository, BusinessService businessService,
                              CustomerProfileRepository customerProfileRepository,
                              CustomerProfileService customerProfileService) {
        this.attendanceRepository = attendanceRepository;
        this.businessService = businessService;
        this.customerProfileRepository = customerProfileRepository;
        this.customerProfileService = customerProfileService;
    }

    /**
     * Opens an attendance record. Serialized on a (business, customer) advisory
     * lock before the duplicate check — the same pattern
     * {@code QueueService#join} uses — so two concurrent check-ins for the same
     * customer cannot both pass a plain "check then insert". The partial unique
     * index added in V9 is the belt-and-braces backstop.
     */
    @Transactional
    public AttendanceDto checkIn(UUID businessId, CreateAttendanceRequest request) {
        businessService.getEntity(businessId);
        UUID customerId = request.customerId();
        if (!customerProfileRepository.existsById(customerId)) {
            throw NotFoundException.of("CustomerProfile", customerId);
        }

        acquireCustomerLock(businessId, customerId);
        if (attendanceRepository.existsByBusinessIdAndCustomerIdAndCheckOutAtIsNull(businessId, customerId)) {
            throw AttendanceConflictException.alreadyCheckedIn();
        }

        customerProfileService.ensureBusinessRelationship(businessId, customerId);
        Attendance attendance = new Attendance(businessId, customerId, Instant.now());
        return toDto(attendanceRepository.save(attendance));
    }

    @Transactional
    public AttendanceDto checkOut(UUID businessId, UUID attendanceId) {
        Attendance attendance = getOwned(businessId, attendanceId);
        if (!attendance.isOpen()) {
            throw AttendanceConflictException.alreadyCheckedOut();
        }
        attendance.checkOut(Instant.now());
        return toDto(attendance);
    }

    @Transactional(readOnly = true)
    public PageResponse<AttendanceDto> list(UUID businessId, boolean activeOnly, Pageable pageable) {
        Page<Attendance> page = activeOnly
                ? attendanceRepository.findByBusinessIdAndCheckOutAtIsNull(businessId, pageable)
                : attendanceRepository.findByBusinessId(businessId, pageable);
        return PageResponse.of(page, AttendanceService::toDto);
    }

    /** Live occupancy: {@code current} counted now, {@code capacity} from the business config. */
    @Transactional(readOnly = true)
    public CapacityResponse capacity(UUID businessId) {
        Business business = businessService.getEntity(businessId);
        long current = attendanceRepository.countByBusinessIdAndCheckOutAtIsNull(businessId);
        return CapacityResponse.of(current, business.getMaxCapacity());
    }

    @Transactional(readOnly = true)
    public Attendance getOwned(UUID businessId, UUID attendanceId) {
        return attendanceRepository.findByIdAndBusinessId(attendanceId, businessId)
                .orElseThrow(() -> NotFoundException.of("Attendance", attendanceId));
    }

    private void acquireCustomerLock(UUID businessId, UUID customerId) {
        long key = Objects.hash(businessId, customerId, "attendance");
        entityManager.createNativeQuery("select pg_advisory_xact_lock(:key)")
                .setParameter("key", key)
                .getSingleResult();
    }

    public static AttendanceDto toDto(Attendance a) {
        return new AttendanceDto(a.getId(), a.getBusinessId(), a.getCustomerId(), a.getCheckInAt(), a.getCheckOutAt());
    }
}
