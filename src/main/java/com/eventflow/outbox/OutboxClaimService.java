package com.eventflow.outbox;

import jakarta.transaction.Transactional;
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
