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
        businessService.getEntity(businessId);
        Service service = catalogService.getOwned(businessId, serviceId);

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

    private AvailabilityResponse unavailable(LocalDate date) {
        return new AvailabilityResponse("UNAVAILABLE", "APPOINTMENT", date, List.of());
    }
}
