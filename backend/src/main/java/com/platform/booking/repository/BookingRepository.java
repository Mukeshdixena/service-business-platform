package com.platform.booking.repository;

import com.platform.booking.domain.Booking;
import com.platform.booking.domain.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Extends {@link JpaSpecificationExecutor} (see {@link BookingSpecifications})
 * for the filtered list endpoints instead of a JPQL query with
 * "(:param is null or field = :param)" placeholders — PostgreSQL's extended
 * query protocol cannot infer a bind parameter's type from an IS-NULL-only
 * usage and fails the whole query with "could not determine data type of
 * parameter" once every optional filter is left unset. Specifications simply
 * omit a predicate entirely when its filter value is absent.
 */
public interface BookingRepository extends JpaRepository<Booking, UUID>, JpaSpecificationExecutor<Booking> {

    Optional<Booking> findByIdAndBusinessId(UUID id, UUID businessId);

    /**
     * All non-terminal bookings for the business overlapping a time window,
     * regardless of staff assignment — callers filter by staffId (including the
     * "no staff assigned" case) in memory. Used by both availability slot
     * generation and the booking-creation conflict check.
     */
    @Query("""
            select b from Booking b
            where b.businessId = :businessId
              and b.status in :statuses
              and b.startAt < :windowEnd
              and b.endAt > :windowStart
            """)
    List<Booking> findOverlapping(@Param("businessId") UUID businessId,
                                   @Param("statuses") List<BookingStatus> statuses,
                                   @Param("windowStart") Instant windowStart,
                                   @Param("windowEnd") Instant windowEnd);
}
