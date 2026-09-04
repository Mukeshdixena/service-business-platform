package com.platform.classes.service;

import com.platform.business.domain.Business;
import com.platform.business.domain.BusinessCapability;
import com.platform.business.service.BusinessService;
import com.platform.classes.domain.ClassEnrollment;
import com.platform.classes.domain.ClassEnrollmentPolicy;
import com.platform.classes.domain.ClassEnrollmentStatus;
import com.platform.classes.domain.ClassSession;
import com.platform.classes.domain.ClassStatus;
import com.platform.classes.dto.ClassDto;
import com.platform.classes.dto.ClassEnrollmentDto;
import com.platform.classes.dto.CreateClassRequest;
import com.platform.classes.dto.UpdateClassRequest;
import com.platform.classes.repository.ClassEnrollmentRepository;
import com.platform.classes.repository.ClassSessionRepository;
import com.platform.classes.repository.ClassSessionSpecifications;
import com.platform.common.exception.NotFoundException;
import com.platform.common.exception.ValidationException;
import com.platform.common.pagination.PageResponse;
import com.platform.customer.service.CustomerProfileService;
import com.platform.staff.service.StaffMemberService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Scheduled classes and their enrollments (CLAUDE_CODE.md §20).
 *
 * <p>Two derived-not-stored rules are enforced here: {@code enrolledCount} is
 * always a live COUNT of ENROLLED enrollments, and a full class WAITLISTS a new
 * enrollment rather than rejecting it (see {@link ClassEnrollmentPolicy}).
 *
 * <p><b>Out of scope this phase:</b> automatic promotion of a WAITLISTED
 * enrollment to ENROLLED when someone cancels. Nothing in API_CONTRACT.md
 * exposes it, and doing it well needs a defined promotion order plus customer
 * notification (Phase 8). A cancelled seat therefore simply frees capacity for
 * the next person who enrolls.
 */
@org.springframework.stereotype.Service
public class ClassService {

    private final ClassSessionRepository classSessionRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final BusinessService businessService;
    private final StaffMemberService staffMemberService;
    private final CustomerProfileService customerProfileService;

    @PersistenceContext
    private EntityManager entityManager;

    public ClassService(ClassSessionRepository classSessionRepository,
                         ClassEnrollmentRepository enrollmentRepository,
                         BusinessService businessService,
                         StaffMemberService staffMemberService,
                         CustomerProfileService customerProfileService) {
        this.classSessionRepository = classSessionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.businessService = businessService;
        this.staffMemberService = staffMemberService;
        this.customerProfileService = customerProfileService;
    }

    // ---------------------------------------------------------------- classes

    @Transactional
    public ClassDto create(UUID businessId, CreateClassRequest request) {
        requireEndAfterStart(request.startAt(), request.endAt());
        if (request.staffId() != null) {
            staffMemberService.getOwned(businessId, request.staffId());
        }
        ClassSession session = new ClassSession(businessId, request.name(), request.description(), request.staffId(),
                request.startAt(), request.endAt(), request.capacity());
        return toDto(classSessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public PageResponse<ClassDto> list(UUID businessId, ClassStatus status, Instant from, Instant to,
                                        Pageable pageable) {
        Specification<ClassSession> spec = ClassSessionSpecifications.businessId(businessId);
        if (status != null) {
            spec = spec.and(ClassSessionSpecifications.status(status));
        }
        if (from != null) {
            spec = spec.and(ClassSessionSpecifications.startAtFrom(from));
        }
        if (to != null) {
            spec = spec.and(ClassSessionSpecifications.startAtTo(to));
        }
        Page<ClassSession> page = classSessionRepository.findAll(spec, pageable);
        return PageResponse.of(page, this::toDto);
    }

    @Transactional
    public ClassDto update(UUID businessId, UUID classId, UpdateClassRequest request) {
        ClassSession session = getOwned(businessId, classId);
        Instant startAt = request.startAt() != null ? request.startAt() : session.getStartAt();
        Instant endAt = request.endAt() != null ? request.endAt() : session.getEndAt();
        requireEndAfterStart(startAt, endAt);
        if (request.staffId() != null) {
            staffMemberService.getOwned(businessId, request.staffId());
        }
        session.applyUpdate(request.name(), request.description(), request.staffId(), request.startAt(),
                request.endAt(), request.capacity(), request.status());
        return toDto(session);
    }

    /** Soft delete: sets CANCELLED — enrollments still reference the class. */
    @Transactional
    public void softDelete(UUID businessId, UUID classId) {
        getOwned(businessId, classId).cancel();
    }

    /**
     * Public/discovery view (API_CONTRACT.md {@code BusinessPublicDto.classes}) —
     * only upcoming SCHEDULED classes. Callers gate on the CLASSES capability.
     */
    @Transactional(readOnly = true)
    public List<ClassDto> listUpcomingPublic(UUID businessId) {
        return classSessionRepository
                .findByBusinessIdAndStatusAndStartAtAfterOrderByStartAtAsc(businessId, ClassStatus.SCHEDULED, Instant.now())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClassSession getOwned(UUID businessId, UUID classId) {
        return classSessionRepository.findByIdAndBusinessId(classId, businessId)
                .orElseThrow(() -> NotFoundException.of("Class", classId));
    }

    // ------------------------------------------------------------ enrollments

    @Transactional(readOnly = true)
    public List<ClassEnrollmentDto> roster(UUID businessId, UUID classId) {
        getOwned(businessId, classId);
        return enrollmentRepository.findByClassIdOrderByCreatedAtAsc(classId).stream()
                .map(ClassService::toDto)
                .toList();
    }

    /**
     * Enrolls a customer. Serialized on a per-class advisory lock (the pattern
     * BookingService/QueueService use) so two simultaneous enrollments into the
     * last remaining seat cannot both read the same pre-full count and both
     * become ENROLLED — the second correctly becomes WAITLISTED.
     */
    @Transactional
    public ClassEnrollmentDto enroll(UUID businessId, UUID classId, UUID customerUserId) {
        Business business = businessService.getEntity(businessId);
        if (!business.getCapabilities().contains(BusinessCapability.CLASSES)) {
            throw new ValidationException("This business does not have the CLASSES capability enabled.");
        }
        ClassSession session = getOwned(businessId, classId);
        if (!session.acceptsEnrollments()) {
            throw new ValidationException("classId: a " + session.getStatus() + " class cannot accept new enrollments.");
        }

        UUID customerProfileId = customerProfileService.getOrCreateEntity(customerUserId).getId();

        acquireClassLock(classId);
        enrollmentRepository.findByClassIdAndCustomerIdAndStatusIn(classId, customerProfileId, activeStatuses())
                .ifPresent(existing -> {
                    throw new ValidationException("You are already enrolled in this class.");
                });

        long enrolledCount = enrollmentRepository.countByClassIdAndStatus(classId, ClassEnrollmentStatus.ENROLLED);
        ClassEnrollmentStatus status = ClassEnrollmentPolicy.statusFor(enrolledCount, session.getCapacity());

        customerProfileService.ensureBusinessRelationship(businessId, customerProfileId);
        ClassEnrollment enrollment = new ClassEnrollment(classId, customerProfileId, status);
        return toDto(enrollmentRepository.save(enrollment));
    }

    /** Customer-initiated cancel — the caller must own the enrollment. */
    @Transactional
    public ClassEnrollmentDto cancelEnrollment(UUID businessId, UUID classId, UUID customerUserId) {
        getOwned(businessId, classId);
        UUID customerProfileId = customerProfileService.getOrCreateEntity(customerUserId).getId();
        ClassEnrollment enrollment = enrollmentRepository
                .findByClassIdAndCustomerIdAndStatusIn(classId, customerProfileId, activeStatuses())
                .orElseThrow(() -> new NotFoundException("No active enrollment found for this class."));
        enrollment.cancel();
        return toDto(enrollment);
    }

    @Transactional
    public ClassEnrollmentDto markAttended(UUID businessId, UUID classId, UUID enrollmentId) {
        ClassEnrollment enrollment = getOwnedEnrollment(businessId, classId, enrollmentId);
        enrollment.markAttended();
        return toDto(enrollment);
    }

    @Transactional
    public ClassEnrollmentDto markNoShow(UUID businessId, UUID classId, UUID enrollmentId) {
        ClassEnrollment enrollment = getOwnedEnrollment(businessId, classId, enrollmentId);
        enrollment.markNoShow();
        return toDto(enrollment);
    }

    @Transactional(readOnly = true)
    public List<ClassEnrollmentDto> listForCustomer(UUID customerProfileId) {
        return enrollmentRepository.findByCustomerIdOrderByCreatedAtDesc(customerProfileId).stream()
                .map(ClassService::toDto)
                .toList();
    }

    private ClassEnrollment getOwnedEnrollment(UUID businessId, UUID classId, UUID enrollmentId) {
        getOwned(businessId, classId);
        return enrollmentRepository.findByIdAndClassId(enrollmentId, classId)
                .orElseThrow(() -> NotFoundException.of("ClassEnrollment", enrollmentId));
    }

    private static EnumSet<ClassEnrollmentStatus> activeStatuses() {
        return EnumSet.of(ClassEnrollmentStatus.ENROLLED, ClassEnrollmentStatus.WAITLISTED);
    }

    private void requireEndAfterStart(Instant startAt, Instant endAt) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new ValidationException("endAt: must be after startAt.");
        }
    }

    private void acquireClassLock(UUID classId) {
        long key = Objects.hash(classId, "class-enrollment");
        entityManager.createNativeQuery("select pg_advisory_xact_lock(:key)")
                .setParameter("key", key)
                .getSingleResult();
    }

    private ClassDto toDto(ClassSession c) {
        long enrolledCount = enrollmentRepository.countByClassIdAndStatus(c.getId(), ClassEnrollmentStatus.ENROLLED);
        return new ClassDto(c.getId(), c.getBusinessId(), c.getName(), c.getDescription(), c.getStaffId(),
                c.getStartAt(), c.getEndAt(), c.getCapacity(), enrolledCount, c.getStatus().name());
    }

    public static ClassEnrollmentDto toDto(ClassEnrollment e) {
        return new ClassEnrollmentDto(e.getId(), e.getClassId(), e.getCustomerId(), e.getStatus().name(),
                e.getCreatedAt());
    }
}
