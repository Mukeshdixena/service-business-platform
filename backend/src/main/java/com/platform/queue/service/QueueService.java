package com.platform.queue.service;

import com.platform.business.domain.Business;
import com.platform.business.domain.BusinessCapability;
import com.platform.business.service.BusinessService;
import com.platform.catalog.domain.Service;
import com.platform.catalog.domain.ServiceBookingType;
import com.platform.catalog.service.CatalogService;
import com.platform.common.exception.NotFoundException;
import com.platform.common.exception.QueueConflictException;
import com.platform.common.exception.ValidationException;
import com.platform.customer.service.CustomerProfileService;
import com.platform.queue.domain.QueueEntry;
import com.platform.queue.domain.QueueEntryStatus;
import com.platform.queue.domain.QueueStateMachine;
import com.platform.queue.dto.CreateQueueEntryRequest;
import com.platform.queue.dto.QueueEntryDto;
import com.platform.queue.dto.QueueStatusResponse;
import com.platform.queue.repository.QueueEntryRepository;
import com.platform.staff.service.StaffMemberService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Business logic for the walk-in queue (CLAUDE_CODE.md §15-16, §35). Position
 * is never stored — {@link #position(UUID, QueueEntry)} always recomputes the
 * 1-based rank by {@code joinedAt} ascending among the business's currently
 * WAITING/CALLED entries, per API_CONTRACT.md.
 *
 * <p>estimatedWaitMinutes assumption: a simple heuristic of
 * (people ahead) x (this entry's own service duration in minutes, defaulting
 * to 15 when the service has none configured) — good enough for an MVP "roughly
 * how long" figure without needing a running average of actually-completed
 * service times.
 */
@org.springframework.stereotype.Service
public class QueueService {

    private static final int DEFAULT_SERVICE_MINUTES = 15;

    private final QueueEntryRepository queueEntryRepository;
    private final BusinessService businessService;
    private final CatalogService catalogService;
    private final StaffMemberService staffMemberService;
    private final CustomerProfileService customerProfileService;

    @PersistenceContext
    private EntityManager entityManager;

    public QueueService(QueueEntryRepository queueEntryRepository, BusinessService businessService,
                         CatalogService catalogService, StaffMemberService staffMemberService,
                         CustomerProfileService customerProfileService) {
        this.queueEntryRepository = queueEntryRepository;
        this.businessService = businessService;
        this.catalogService = catalogService;
        this.staffMemberService = staffMemberService;
        this.customerProfileService = customerProfileService;
    }

    @Transactional
    public QueueEntryDto join(UUID businessId, UUID customerUserId, CreateQueueEntryRequest request) {
        Business business = businessService.getEntity(businessId);
        if (!business.getCapabilities().contains(BusinessCapability.QUEUE)) {
            throw new ValidationException("This business does not have the QUEUE capability enabled.");
        }

        Service service = catalogService.getOwned(businessId, request.serviceId());
        if (!service.isActive()) {
            throw new ValidationException("serviceId: service is not active.");
        }
        if (service.getBookingType() != ServiceBookingType.QUEUE && service.getBookingType() != ServiceBookingType.WALK_IN) {
            throw new ValidationException("serviceId: only QUEUE or WALK_IN services can be joined via the queue.");
        }

        UUID staffId = request.staffId();
        if (staffId != null) {
            var staff = staffMemberService.getOwned(businessId, staffId);
            if (!staff.isActive()) {
                throw new ValidationException("staffId: staff member is not active.");
            }
        }

        UUID customerProfileId = customerProfileService.getOrCreateEntity(customerUserId).getId();

        // Serialize concurrent joins for the same (business, customer) so the
        // duplicate-active-entry check below cannot race — same pattern as
        // BookingService's advisory-lock-based slot check.
        acquireCustomerLock(businessId, customerProfileId);
        if (queueEntryRepository.existsByBusinessIdAndCustomerIdAndStatusIn(
                businessId, customerProfileId, QueueStateMachine.activeStatuses())) {
            throw QueueConflictException.alreadyActive();
        }

        customerProfileService.ensureBusinessRelationship(businessId, customerProfileId);

        QueueEntry entry = new QueueEntry(businessId, customerProfileId, service.getId(), staffId);
        entry = queueEntryRepository.save(entry);
        return toDto(entry, service.getDurationMinutes());
    }

    @Transactional(readOnly = true)
    public List<QueueEntryDto> listForBusiness(UUID businessId, QueueEntryStatus status) {
        List<QueueEntry> entries = status != null
                ? queueEntryRepository.findByBusinessIdAndStatusOrderByJoinedAtAsc(businessId, status)
                : queueEntryRepository.findByBusinessIdAndStatusInOrderByJoinedAtAsc(businessId,
                        List.of(QueueEntryStatus.values()));
        return entries.stream()
                .sorted(Comparator.comparing(QueueEntry::getJoinedAt))
                .map(e -> toDto(e, serviceDurationOrDefault(e)))
                .toList();
    }

    @Transactional
    public QueueEntryDto call(UUID businessId, UUID id) {
        QueueEntry entry = getOwned(businessId, id);
        entry.transitionTo(QueueEntryStatus.CALLED);
        return toDto(entry, serviceDurationOrDefault(entry));
    }

    @Transactional
    public QueueEntryDto start(UUID businessId, UUID id) {
        QueueEntry entry = getOwned(businessId, id);
        entry.transitionTo(QueueEntryStatus.SERVING);
        return toDto(entry, serviceDurationOrDefault(entry));
    }

    @Transactional
    public QueueEntryDto complete(UUID businessId, UUID id) {
        QueueEntry entry = getOwned(businessId, id);
        entry.transitionTo(QueueEntryStatus.COMPLETED);
        return toDto(entry, serviceDurationOrDefault(entry));
    }

    @Transactional
    public QueueEntryDto skip(UUID businessId, UUID id) {
        QueueEntry entry = getOwned(businessId, id);
        entry.transitionTo(QueueEntryStatus.SKIPPED);
        return toDto(entry, serviceDurationOrDefault(entry));
    }

    @Transactional
    public QueueEntryDto noShow(UUID businessId, UUID id) {
        QueueEntry entry = getOwned(businessId, id);
        entry.transitionTo(QueueEntryStatus.NO_SHOW);
        return toDto(entry, serviceDurationOrDefault(entry));
    }

    @Transactional
    public QueueEntryDto cancelByBusiness(UUID businessId, UUID id) {
        QueueEntry entry = getOwned(businessId, id);
        entry.transitionTo(QueueEntryStatus.CANCELLED);
        return toDto(entry, serviceDurationOrDefault(entry));
    }

    /** Customer-initiated cancel — the caller must own the entry (via their customer profile). */
    @Transactional
    public QueueEntryDto cancelByCustomer(UUID id, UUID customerProfileId) {
        QueueEntry entry = queueEntryRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("QueueEntry", id));
        if (!entry.getCustomerId().equals(customerProfileId)) {
            throw new com.platform.common.exception.ForbiddenException(
                    "You do not have permission to cancel this queue entry.");
        }
        entry.transitionTo(QueueEntryStatus.CANCELLED);
        return toDto(entry, serviceDurationOrDefault(entry));
    }

    @Transactional(readOnly = true)
    public List<QueueEntryDto> listForCustomer(UUID customerProfileId) {
        return queueEntryRepository.findByCustomerIdOrderByJoinedAtDesc(customerProfileId).stream()
                .map(e -> toDto(e, serviceDurationOrDefault(e)))
                .toList();
    }

    @Transactional(readOnly = true)
    public QueueStatusResponse statusForCustomer(UUID id, UUID customerProfileId) {
        QueueEntry entry = queueEntryRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("QueueEntry", id));
        if (!entry.getCustomerId().equals(customerProfileId)) {
            throw new com.platform.common.exception.ForbiddenException(
                    "You do not have permission to view this queue entry.");
        }
        int peopleAhead = Math.max(0, position(entry.getBusinessId(), entry) - 1);
        int minutes = peopleAhead * serviceDurationOrDefault(entry);
        return new QueueStatusResponse(entry.getStatus().name(), peopleAhead, minutes);
    }

    @Transactional(readOnly = true)
    public QueueEntry getOwned(UUID businessId, UUID id) {
        return queueEntryRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> NotFoundException.of("QueueEntry", id));
    }

    /**
     * 1-based rank by joinedAt ascending among the business's currently
     * WAITING/CALLED entries. SERVING/terminal entries are not "positioned" in
     * the waiting line, so this returns 0 for them.
     */
    private int position(UUID businessId, QueueEntry entry) {
        if (entry.getStatus() != QueueEntryStatus.WAITING && entry.getStatus() != QueueEntryStatus.CALLED) {
            return 0;
        }
        List<QueueEntry> active = queueEntryRepository.findByBusinessIdAndStatusInOrderByJoinedAtAsc(
                businessId, Set.of(QueueEntryStatus.WAITING, QueueEntryStatus.CALLED));
        int rank = 1;
        for (QueueEntry e : active) {
            if (e.getId().equals(entry.getId())) {
                return rank;
            }
            rank++;
        }
        return 0;
    }

    private int serviceDurationOrDefault(QueueEntry entry) {
        try {
            Service service = catalogService.getOwned(entry.getBusinessId(), entry.getServiceId());
            Integer minutes = service.getDurationMinutes();
            return minutes != null && minutes > 0 ? minutes : DEFAULT_SERVICE_MINUTES;
        } catch (NotFoundException ex) {
            return DEFAULT_SERVICE_MINUTES;
        }
    }

    private void acquireCustomerLock(UUID businessId, UUID customerProfileId) {
        long key = Objects.hash(businessId, customerProfileId, "queue");
        entityManager.createNativeQuery("select pg_advisory_xact_lock(:key)")
                .setParameter("key", key)
                .getSingleResult();
    }

    private QueueEntryDto toDto(QueueEntry e, int serviceDurationMinutes) {
        int pos = position(e.getBusinessId(), e);
        int peopleAhead = Math.max(0, pos - 1);
        int estimatedWaitMinutes = peopleAhead * serviceDurationMinutes;
        return new QueueEntryDto(e.getId(), e.getBusinessId(), e.getCustomerId(), e.getServiceId(), e.getStaffId(),
                pos, e.getStatus().name(), e.getJoinedAt(), e.getCalledAt(), e.getStartedAt(), e.getCompletedAt(),
                estimatedWaitMinutes);
    }
}
