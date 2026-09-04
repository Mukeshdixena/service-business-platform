package com.platform.queue.repository;

import com.platform.queue.domain.QueueEntry;
import com.platform.queue.domain.QueueEntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, UUID> {

    Optional<QueueEntry> findByIdAndBusinessId(UUID id, UUID businessId);

    List<QueueEntry> findByBusinessIdAndStatusOrderByJoinedAtAsc(UUID businessId, QueueEntryStatus status);

    List<QueueEntry> findByBusinessIdAndStatusInOrderByJoinedAtAsc(UUID businessId, Collection<QueueEntryStatus> statuses);

    boolean existsByBusinessIdAndCustomerIdAndStatusIn(UUID businessId, UUID customerId, Collection<QueueEntryStatus> statuses);

    List<QueueEntry> findByCustomerIdOrderByJoinedAtDesc(UUID customerId);
}
