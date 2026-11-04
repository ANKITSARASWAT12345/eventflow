package com.eventflow.outbox;


import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class OutboxCompletionService {


    private  final EventOutboxRepository outboxRepository;

    public OutboxCompletionService(
            EventOutboxRepository outboxRepository
    ) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public boolean markPublished(UUID eventId, UUID claimToken) {

        int updated = outboxRepository.markPublishedIfClaimed(
                eventId,
                claimToken,
                Instant.now()
        );

        return updated == 1;
    }


    @Transactional
    public boolean markFailure(
            UUID eventId,
            UUID claimToken,
            int currentAttempts,
            String error
    ) {

        int nextAttempt = currentAttempts + 1;

        Instant nextRetryAt = null;

        if (nextAttempt < 5) {
            long delaySeconds =
                    (long) Math.pow(2, nextAttempt) * 10;

            nextRetryAt = Instant.now().plusSeconds(delaySeconds);
        }

        int updated = outboxRepository.markFailureIfClaimed(
                eventId,
                claimToken,
                error,
                nextRetryAt
        );

        return updated == 1;
    }


}
