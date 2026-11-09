package com.eventflow.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class EventOutboxService {

    private final EventOutboxRepository outboxRepository;

    public EventOutboxService(
            EventOutboxRepository outboxRepository
    ) {
        this.outboxRepository = outboxRepository;
    }


    @Transactional
    public List<EventOutbox> claimPendingBatch(int batchSize) {

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "Batch size must be greater than zero"
            );
        }

        Instant now = Instant.now();

        // Step 1: Lock and fetch eligible pending events.
        List<EventOutbox> events =
                outboxRepository.claimPendingEvents(
                        now,
                        batchSize
                );

        // Step 2: Change each event to PROCESSING.
        // Each event gets its own unique claim token.
        for (EventOutbox event : events) {
            event.markProcessing();
        }

        // Step 3: Persist state changes within the
        // same transaction as the row-locking query.
        outboxRepository.saveAll(events);

        return events;
    }

}
