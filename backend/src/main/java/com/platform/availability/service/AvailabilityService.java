package com.platform.availability.service;

import com.platform.availability.dto.AvailabilityResponse;
import com.platform.availability.dto.SlotDto;
import com.platform.booking.domain.Booking;
import com.platform.booking.domain.BookingStatus;
import com.platform.booking.repository.BookingRepository;
import com.platform.business.repository.BusinessHoursRepository;
import com.platform.business.service.BusinessService;
import com.platform.catalog.domain.Service;
import com.platform.catalog.domain.ServiceBookingType;
import com.platform.catalog.service.CatalogService;
import com.platform.staff.domain.StaffMember;
import com.platform.staff.service.StaffMemberService;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Orchestrates appointment availability (CLAUDE_CODE.md §13/§36): business
 * hours + service duration + staff assignment + existing non-terminal bookings.
 * The actual slot math lives in the pure {@link SlotCalculator}; this class is
 * only responsible for loading the right data and picking which "track"
 * (per-staff calendar, or one shared calendar when no staff is required) to
 * generate slots for.
 */
@org.springframework.stereotype.Service
public class AvailabilityService {

    private final BusinessService businessService;
    private final CatalogService catalogService;
    private final StaffMemberService staffMemberService;
    private final BusinessHoursRepository businessHoursRepository;
    private final BookingRepository bookingRepository;

    public AvailabilityService(BusinessService businessService, CatalogService catalogService,
                                StaffMemberService staffMemberService,
                                BusinessHoursRepository businessHoursRepository,
                                BookingRepository bookingRepository) {
        this.businessService = businessService;
        this.catalogService = catalogService;
        this.staffMemberService = staffMemberService;
        this.businessHoursRepository = businessHoursRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(UUID businessId, UUID serviceId, UUID staffId, LocalDate date) {
        return getAvailability(businessId, serviceId, staffId, null, date);
    }

    /**
     * {@code resourceId} is the rental counterpart of {@code staffId}
     * (API_CONTRACT.md "Rental availability reuses the existing availability
     * endpoint"): same response shape, but slots are generated against the
     * resource's existing rental bookings instead of a staff member's
     * appointments.
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(UUID businessId, UUID serviceId, UUID staffId, UUID resourceId,
                                                 LocalDate date) {
        businessService.getEntity(businessId);
        Service service = catalogService.getOwned(businessId, serviceId);

        if (resourceId != null || service.getBookingType() == ServiceBookingType.RENTAL) {
            return rentalAvailability(businessId, service, resourceId, date);
        }

        if (!service.isActive() || service.getBookingType() != ServiceBookingType.APPOINTMENT
                || service.getDurationMinutes() == null || service.getDurationMinutes() <= 0) {
            return unavailable(date);
        }

        List<StaffMember> tracks;
        if (staffId != null) {
            StaffMember staff = staffMemberService.getOwned(businessId, staffId);
            if (!staff.isActive()) {
                return unavailable(date);
            }
            tracks = List.of(staff);
        } else {
            tracks = staffMemberService.findActiveAssignedToService(businessId, serviceId);
        }

        List<SlotCalculator.OpenInterval> openIntervals = businessHoursRepository
                .findByBusinessIdAndDayOfWeekOrderByOpenTimeAsc(businessId, date.getDayOfWeek()).stream()
                .map(h -> new SlotCalculator.OpenInterval(h.getOpenTime(), h.getCloseTime()))
                .toList();

        Instant now = Instant.now();
        Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<Booking> dayBookings = bookingRepository.findOverlapping(businessId,
                List.copyOf(BookingStatus.nonTerminalStatuses()), dayStart, dayEnd);

        List<SlotDto> allSlots = new ArrayList<>();
        if (tracks.isEmpty()) {
            // No staff assignment required/available for this service — fall back to a
            // single shared business calendar (staffId = null slots).
            List<SlotCalculator.BookedInterval> booked = dayBookings.stream()
                    .filter(b -> b.getStaffId() == null)
                    .map(b -> new SlotCalculator.BookedInterval(b.getStartAt(), b.getEndAt()))
                    .toList();
            allSlots.addAll(SlotCalculator.generate(date, openIntervals, service.getDurationMinutes(), booked, now, null));
        } else {
            for (StaffMember staff : tracks) {
                List<SlotCalculator.BookedInterval> booked = dayBookings.stream()
                        .filter(b -> Objects.equals(b.getStaffId(), staff.getId()))
                        .map(b -> new SlotCalculator.BookedInterval(b.getStartAt(), b.getEndAt()))
                        .toList();
                allSlots.addAll(SlotCalculator.generate(date, openIntervals, service.getDurationMinutes(), booked, now, staff.getId()));
            }
        }

        boolean anyAvailable = allSlots.stream().anyMatch(SlotDto::available);
        return new AvailabilityResponse(anyAvailable ? "AVAILABLE" : "UNAVAILABLE", "APPOINTMENT", date, allSlots);
    }

    /**
     * Rental slots for one resource on one day. The day is treated as a single
     * 00:00-24:00 window rather than being clipped to business hours: a rental
     * legitimately runs overnight/over several days, so opening hours constrain
     * when the customer collects the asset, not when they may hold it (this
     * matches BookingService, which skips the business-hours check for rentals).
     * Slot length is the service's pricing unit — hourly rentals get hourly
     * slots, daily rentals a single whole-day slot.
     */
    private AvailabilityResponse rentalAvailability(UUID businessId, Service service, UUID resourceId, LocalDate date) {
        if (resourceId == null || !service.isActive() || service.getBookingType() != ServiceBookingType.RENTAL
                || service.getPricingUnit() == null) {
            return unavailable(date, "RENTAL");
        }
        Resource resource = resourceService.getOwned(businessId, resourceId);
        if (!resource.isBookable()) {
            return unavailable(date, "RENTAL");
        }

        Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<SlotCalculator.BookedInterval> booked = bookingRepository.findOverlapping(businessId,
                        List.copyOf(BookingStatus.nonTerminalStatuses()), dayStart, dayEnd).stream()
                .filter(b -> Objects.equals(b.getResourceId(), resourceId))
                .map(b -> new SlotCalculator.BookedInterval(b.getStartAt(), b.getEndAt()))
                .toList();

        List<SlotCalculator.OpenInterval> wholeDay =
                List.of(new SlotCalculator.OpenInterval(LocalTime.MIDNIGHT, LocalTime.MAX));
        List<SlotDto> slots = SlotCalculator.generate(date, wholeDay, service.getPricingUnit().minutes(), booked,
                Instant.now(), null);

        boolean anyAvailable = slots.stream().anyMatch(SlotDto::available);
        return new AvailabilityResponse(anyAvailable ? "AVAILABLE" : "UNAVAILABLE", "RENTAL", date, slots);
    }

    private AvailabilityResponse unavailable(LocalDate date) {
        return unavailable(date, "APPOINTMENT");
    }

    private AvailabilityResponse unavailable(LocalDate date, String type) {
        return new AvailabilityResponse("UNAVAILABLE", type, date, List.of());
    }
}
