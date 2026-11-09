package com.eventflow.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventOutboxRepository extends JpaRepository<EventOutbox, UUID> {


    Optional<EventOutbox> findByEventId(UUID eventId);


//    @Query(value = """
//    SELECT *
//    FROM event_outbox
//    WHERE status = :status
//      AND (next_retry_at IS NULL OR next_retry_at <= :now)
//    ORDER BY created_at ASC
//    LIMIT :batchSize
//    FOR UPDATE SKIP LOCKED
//    """, nativeQuery = true)
//    List<EventOutbox> findEligiblePendingEvents(
//            @Param("status") String status,
//            @Param("now") Instant now,
//            @Param("batchSize") int batchSize
//    );


    @Query(value = """
    SELECT *
    FROM event_outbox
    WHERE status = 'PENDING'
      AND (next_retry_at IS NULL OR next_retry_at <= :now)
    ORDER BY created_at ASC
    LIMIT :batchSize
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<EventOutbox> claimPendingEvents(
            @Param("now") Instant now,
            @Param("batchSize") int batchSize
    );

//    @Query(value = """
//    SELECT *
//    FROM event_outbox
//    WHERE status = 'PROCESSING'
//      AND locked_at <= :leaseExpiry
//    ORDER BY locked_at ASC
//    """, nativeQuery = true)
//    List<EventOutbox> findExpiredClaims(
//            @Param("leaseExpiry") Instant leaseExpiry
//    );


    @Query(value = """
    SELECT *
    FROM event_outbox
    WHERE status = 'PROCESSING'
      AND locked_at <= :leaseExpiry
    ORDER BY locked_at ASC
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<EventOutbox> findExpiredClaims(
            @Param("leaseExpiry") Instant leaseExpiry
    );



    @Modifying(flushAutomatically = true,
            clearAutomatically = true)
    @Query(value = """
    UPDATE event_outbox
    SET status = 'PUBLISHED',
        published_at = :now,
        last_error = NULL,
        next_retry_at = NULL,
        locked_at = NULL,
        claim_token = NULL
    WHERE event_id = :eventId
      AND status = 'PROCESSING'
      AND claim_token = :claimToken
    """, nativeQuery = true)
    int markPublishedIfClaimed(
            @Param("eventId") UUID eventId,
            @Param("claimToken") UUID claimToken,
            @Param("now") Instant now
    );

    @Modifying(flushAutomatically = true,
            clearAutomatically = true)
    @Query(value = """
    UPDATE event_outbox
    SET attempts = attempts + 1,
        last_error = :error,
        status = CASE
            WHEN attempts + 1 >= 5 THEN 'FAILED'
            ELSE 'PENDING'
        END,
        next_retry_at = CASE
            WHEN attempts + 1 >= 5 THEN NULL
            ELSE CAST(:nextRetryAt AS TIMESTAMP WITH TIME ZONE)
        END,
        locked_at = NULL,
        claim_token = NULL
    WHERE event_id = :eventId
      AND status = 'PROCESSING'
      AND claim_token = :claimToken
    """, nativeQuery = true)
    int markFailureIfClaimed(
            @Param("eventId") UUID eventId,
            @Param("claimToken") UUID claimToken,
            @Param("error") String error,
            @Param("nextRetryAt") Instant nextRetryAt
    );


}
