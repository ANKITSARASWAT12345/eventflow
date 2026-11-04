package com.eventflow.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventOutboxRepository extends JpaRepository<EventOutbox, UUID> {


    Optional<EventOutbox> findByEventId(UUID eventId);


    @Query(value = """
    SELECT *
    FROM event_outbox
    WHERE status = :status
      AND (next_retry_at IS NULL OR next_retry_at <= :now)
    ORDER BY created_at ASC
    LIMIT :batchSize
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<EventOutbox> findEligiblePendingEvents(
            @Param("status") String status,
            @Param("now") Instant now,
            @Param("batchSize") int batchSize
    );

}
