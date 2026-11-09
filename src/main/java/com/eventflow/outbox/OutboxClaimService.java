package com.eventflow.outbox;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class OutboxClaimService {

    private static final Duration LEASE_DURATION = Duration.ofMinutes(2);

    private final EventOutboxRepository outboxRepository;

    public OutboxClaimService(EventOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public List<EventOutbox> claimPendingEvents(int batchSize) {

        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be greater than zero");
        }

        List<EventOutbox> events = outboxRepository.claimPendingEvents(
                Instant.now(),
                batchSize
        );

        for (EventOutbox event : events) {
            event.markProcessing();
        }

        return events;
    }

    @Transactional
    public int recoverExpiredClaims() {

        Instant leaseExpiry = Instant.now().minus(LEASE_DURATION);

        List<EventOutbox> expiredEvents =
                outboxRepository.findExpiredClaims(leaseExpiry);

        for (EventOutbox event : expiredEvents) {
            event.recoverExpiredClaim();
        }

        return expiredEvents.size();
    }
}
