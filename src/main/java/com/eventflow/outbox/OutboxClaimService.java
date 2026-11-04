package com.eventflow.outbox;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class OutboxClaimService {

    private final EventOutboxRepository outboxRepository;

    public OutboxClaimService(EventOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public List<EventOutbox> claimPendingEvents(int batchSize) {
        return outboxRepository.findEligiblePendingEvents(
                "PENDING",
                Instant.now(),
                batchSize
        );
    }
}
