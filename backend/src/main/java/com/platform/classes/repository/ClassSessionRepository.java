package com.platform.classes.repository;

import com.platform.classes.domain.ClassSession;
import com.platform.classes.domain.ClassStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Uses {@link JpaSpecificationExecutor} for the filtered list endpoint for the
 * same reason {@code BookingRepository} does — PostgreSQL cannot infer the type
 * of a bind parameter used only in an IS-NULL comparison, so
 * "(:param is null or ...)" JPQL breaks once all optional filters are unset.
 */
public interface ClassSessionRepository extends JpaRepository<ClassSession, UUID>,
        JpaSpecificationExecutor<ClassSession> {

    Optional<ClassSession> findByIdAndBusinessId(UUID id, UUID businessId);

    Page<ClassSession> findByBusinessId(UUID businessId, Pageable pageable);

    List<ClassSession> findByBusinessIdAndStatusAndStartAtAfterOrderByStartAtAsc(UUID businessId, ClassStatus status,
                                                                                  Instant after);
}
