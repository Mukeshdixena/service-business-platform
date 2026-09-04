package com.platform.booking.service;

import com.platform.booking.domain.Booking;
import com.platform.booking.domain.BookingConflictDetector;
import com.platform.booking.domain.RentalPricing;
import com.platform.booking.domain.BookingStatus;
import com.platform.booking.dto.BookingDto;
import com.platform.booking.dto.CreateBookingRequest;
import com.platform.booking.repository.BookingRepository;
import com.platform.booking.repository.BookingSpecifications;
import com.platform.business.domain.Business;
import com.platform.business.domain.BusinessHours;
import com.platform.business.domain.BusinessStatus;
import com.platform.business.repository.BusinessHoursRepository;
import com.platform.business.service.BusinessService;
import com.platform.catalog.domain.Service;
import com.platform.catalog.domain.ServiceBookingType;
import com.platform.catalog.service.CatalogService;
import com.platform.common.exception.BookingConflictException;
import com.platform.common.exception.ForbiddenException;
import com.platform.common.exception.NotFoundException;
import com.platform.common.exception.ValidationException;
import com.platform.common.pagination.PageResponse;
import com.platform.customer.service.CustomerProfileService;
import com.platform.resource.domain.Resource;
import com.platform.resource.service.ResourceService;
import com.platform.staff.domain.StaffMember;
import com.platform.staff.service.StaffMemberService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@org.springframework.stereotype.Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BusinessService businessService;
    private final CatalogService catalogService;
    private final StaffMemberService staffMemberService;
    private final BusinessHoursRepository businessHoursRepository;
    private final CustomerProfileService customerProfileService;
    private final ResourceService resourceService;

    @PersistenceContext
    private EntityManager entityManager;

    public BookingService(BookingRepository bookingRepository, BusinessService businessService,
                           CatalogService catalogService, StaffMemberService staffMemberService,
                           BusinessHoursRepository businessHoursRepository,
                           CustomerProfileService customerProfileService,
                           ResourceService resourceService) {
        this.bookingRepository = bookingRepository;
        this.businessService = businessService;
        this.catalogService = catalogService;
        this.staffMemberService = staffMemberService;
        this.businessHoursRepository = businessHoursRepository;
        this.customerProfileService = customerProfileService;
        this.resourceService = resourceService;
    }

    /**
     * Creates a booking as PENDING. {@code endAt}/{@code price}/{@code currency}
     * are always computed from the {@code Service}, never accepted from the
     * client. Wrapped in a single transaction that takes a Postgres advisory
     * lock scoped to (business, staff) BEFORE re-checking for overlaps: two
     * concurrent requests for the same slot serialize on the lock, so the second
     * one to acquire it sees the first's already-committed booking and is
     * correctly rejected — a plain "check then insert" without the lock would
     * have a race window where both requests pass the check.
     */
    @Transactional
    public BookingDto create(UUID businessId, UUID customerUserId, CreateBookingRequest request) {
        Business business = businessService.getEntity(businessId);
        if (business.getStatus() != BusinessStatus.ACTIVE) {
            throw new ValidationException("Business is not currently accepting bookings.");
        }
        Service service = catalogService.getOwned(businessId, request.serviceId());
        if (!service.isActive()) {
            throw new ValidationException("serviceId: service is not active.");
        }
        boolean rental = service.getBookingType() == ServiceBookingType.RENTAL;
        if (!rental && service.getBookingType() != ServiceBookingType.APPOINTMENT) {
            throw new ValidationException(
                    "serviceId: only APPOINTMENT and RENTAL services support booking in this phase.");
        }

        Instant startAt = request.startAt();
        UUID staffId;
        UUID resourceId;
        Instant endAt;
        java.math.BigDecimal price;

        if (rental) {
            // Rentals are keyed on a resource and priced by customer-chosen duration.
            if (request.staffId() != null) {
                throw new ValidationException("staffId: must not be set for a RENTAL booking.");
            }
            if (request.resourceId() == null) {
                throw new ValidationException("resourceId: must not be null for a RENTAL booking.");
            }
            if (request.endAt() == null) {
                throw new ValidationException("endAt: must not be null for a RENTAL booking.");
            }
            if (!request.endAt().isAfter(startAt)) {
                throw new ValidationException("endAt: must be after startAt.");
            }
            if (service.getPricingUnit() == null) {
                throw new ValidationException("serviceId: RENTAL service has no pricingUnit configured.");
            }
            Resource resource = resourceService.getOwned(businessId, request.resourceId());
            if (!resource.isBookable()) {
                throw new ValidationException("resourceId: resource is " + resource.getStatus()
                        + " and cannot be booked.");
            }
            staffId = null;
            resourceId = resource.getId();
            endAt = request.endAt();
            price = RentalPricing.totalPrice(service.getPrice(), service.getPricingUnit(), startAt, endAt);
        } else {
            if (request.resourceId() != null) {
                throw new ValidationException("resourceId: must not be set for an APPOINTMENT booking.");
            }
            if (service.getDurationMinutes() == null || service.getDurationMinutes() <= 0) {
                throw new ValidationException("serviceId: service has no valid duration configured.");
            }
            staffId = request.staffId();
            if (staffId != null) {
                StaffMember staff = staffMemberService.getOwned(businessId, staffId);
                if (!staff.isActive()) {
                    throw new ValidationException("staffId: staff member is not active.");
                }
            }
            resourceId = null;
            endAt = startAt.plusSeconds(service.getDurationMinutes() * 60L);
            price = service.getPrice();
            // Business hours constrain a fixed-length appointment slot; a multi-day
            // rental legitimately spans closing time, so §33's hours check is
            // appointment-only.
            requireWithinBusinessHours(businessId, startAt, endAt);
        }

        acquireSlotLock(businessId, staffId, resourceId);
        requireNoOverlap(businessId, staffId, resourceId, startAt, endAt);

        UUID customerProfileId = customerProfileService.getOrCreateEntity(customerUserId).getId();
        customerProfileService.ensureBusinessRelationship(businessId, customerProfileId);

        Booking booking = new Booking(businessId, customerProfileId, service.getId(), staffId, resourceId, startAt,
                endAt, price, service.getCurrency(), request.notes());
        return toDto(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingDto> listForBusiness(UUID businessId, BookingStatus status, Instant from, Instant to,
                                                      Pageable pageable) {
        Specification<Booking> spec = buildFilterSpec(BookingSpecifications.businessId(businessId), status, from, to);
        Page<Booking> page = bookingRepository.findAll(spec, pageable);
        return PageResponse.of(page, BookingService::toDto);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingDto> listForCustomer(UUID customerProfileId, BookingStatus status, Instant from,
                                                      Instant to, Pageable pageable) {
        Specification<Booking> spec = buildFilterSpec(BookingSpecifications.customerId(customerProfileId), status, from, to);
        Page<Booking> page = bookingRepository.findAll(spec, pageable);
        return PageResponse.of(page, BookingService::toDto);
    }

    private Specification<Booking> buildFilterSpec(Specification<Booking> base, BookingStatus status,
                                                     Instant from, Instant to) {
        Specification<Booking> spec = base;
        if (status != null) {
            spec = spec.and(BookingSpecifications.status(status));
        }
        if (from != null) {
            spec = spec.and(BookingSpecifications.startAtFrom(from));
        }
        if (to != null) {
            spec = spec.and(BookingSpecifications.startAtTo(to));
        }
        return spec;
    }

    @Transactional
    public BookingDto confirm(UUID businessId, UUID bookingId) {
        Booking booking = getOwned(businessId, bookingId);
        booking.transitionTo(BookingStatus.CONFIRMED);
        return toDto(booking);
    }

    @Transactional
    public BookingDto checkIn(UUID businessId, UUID bookingId) {
        Booking booking = getOwned(businessId, bookingId);
        booking.transitionTo(BookingStatus.CHECKED_IN);
        return toDto(booking);
    }

    @Transactional
    public BookingDto start(UUID businessId, UUID bookingId) {
        Booking booking = getOwned(businessId, bookingId);
        booking.transitionTo(BookingStatus.IN_PROGRESS);
        return toDto(booking);
    }

    @Transactional
    public BookingDto complete(UUID businessId, UUID bookingId) {
        Booking booking = getOwned(businessId, bookingId);
        booking.transitionTo(BookingStatus.COMPLETED);
        return toDto(booking);
    }

    @Transactional
    public BookingDto cancelByBusiness(UUID businessId, UUID bookingId, String reason) {
        Booking booking = getOwned(businessId, bookingId);
        booking.transitionTo(BookingStatus.CANCELLED, reason);
        return toDto(booking);
    }

    @Transactional
    public BookingDto noShow(UUID businessId, UUID bookingId) {
        Booking booking = getOwned(businessId, bookingId);
        booking.transitionTo(BookingStatus.NO_SHOW);
        return toDto(booking);
    }

    @Transactional
    public BookingDto reject(UUID businessId, UUID bookingId, String reason) {
        Booking booking = getOwned(businessId, bookingId);
        booking.transitionTo(BookingStatus.REJECTED, reason);
        return toDto(booking);
    }

    /** Customer-initiated cancel — the caller must own the booking (via their customer profile). */
    @Transactional
    public BookingDto cancelByCustomer(UUID bookingId, UUID customerProfileId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> NotFoundException.of("Booking", bookingId));
        if (!booking.getCustomerId().equals(customerProfileId)) {
            throw new ForbiddenException("You do not have permission to cancel this booking.");
        }
        booking.transitionTo(BookingStatus.CANCELLED, reason);
        return toDto(booking);
    }

    @Transactional(readOnly = true)
    public Booking getOwned(UUID businessId, UUID bookingId) {
        return bookingRepository.findByIdAndBusinessId(bookingId, businessId)
                .orElseThrow(() -> NotFoundException.of("Booking", bookingId));
    }

    private void requireWithinBusinessHours(UUID businessId, Instant startAt, Instant endAt) {
        var startDate = startAt.atZone(ZoneOffset.UTC).toLocalDate();
        List<BusinessHours> hours = businessHoursRepository
                .findByBusinessIdAndDayOfWeekOrderByOpenTimeAsc(businessId, startDate.getDayOfWeek());
        boolean withinHours = hours.stream().anyMatch(h -> {
            Instant openInstant = startDate.atTime(h.getOpenTime()).toInstant(ZoneOffset.UTC);
            Instant closeInstant = startDate.atTime(h.getCloseTime()).toInstant(ZoneOffset.UTC);
            return !startAt.isBefore(openInstant) && !endAt.isAfter(closeInstant);
        });
        if (!withinHours) {
            throw new ValidationException("startAt: requested time is outside business hours.");
        }
    }

    /**
     * CLAUDE_CODE.md §33/§34. Identical rule for both booking kinds — the only
     * difference is whether the "track" being checked is a staff member's
     * calendar or a resource's (see {@link BookingConflictDetector}).
     */
    private void requireNoOverlap(UUID businessId, UUID staffId, UUID resourceId, Instant startAt, Instant endAt) {
        List<Booking> candidates = bookingRepository.findOverlapping(businessId,
                List.copyOf(BookingStatus.nonTerminalStatuses()), startAt, endAt);
        if (BookingConflictDetector.conflicts(startAt, endAt, staffId, resourceId, candidates)) {
            throw BookingConflictException.slotTaken();
        }
    }

    /**
     * Serializes concurrent booking attempts for the same (business, staff) or
     * (business, resource) pair — a rental for a given JCB serializes exactly
     * like an appointment for a given stylist.
     * A Postgres transaction-scoped advisory lock is used instead of
     * SELECT ... FOR UPDATE because there is no existing row to lock before the
     * first booking in a slot is inserted. The lock is released automatically
     * at transaction commit/rollback.
     */
    private void acquireSlotLock(UUID businessId, UUID staffId, UUID resourceId) {
        long key = lockKey(businessId, staffId, resourceId);
        entityManager.createNativeQuery("select pg_advisory_xact_lock(:key)")
                .setParameter("key", key)
                .getSingleResult();
    }

    private long lockKey(UUID businessId, UUID staffId, UUID resourceId) {
        return Objects.hash(businessId, staffId, resourceId);
    }

    public static BookingDto toDto(Booking b) {
        return new BookingDto(b.getId(), b.getBusinessId(), b.getCustomerId(), b.getServiceId(), b.getStaffId(),
                b.getResourceId(), b.getStartAt(), b.getEndAt(), b.getStatus().name(), b.getPrice(), b.getCurrency(), b.getNotes(),
                b.getCreatedAt(), b.getUpdatedAt());
    }
}
