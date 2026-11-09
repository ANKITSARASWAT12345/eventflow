package com.eventflow.outbox;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class OutboxStateService {

    private  static final int MAX_ATTEMPTS=5;
    private final EventOutboxRepository outboxRepository;

    public OutboxStateService(
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
    public boolean markFailed(
            UUID eventId,
            UUID claimToken,
            int currentAttempts,
            String error
    ) {

        int nextAttempt = currentAttempts + 1;

        Instant nextRetryAt = null;

        if (nextAttempt < MAX_ATTEMPTS) {



            long delaySeconds = 10L * (1L << nextAttempt);

            nextRetryAt = Instant.now().plusSeconds(delaySeconds);
        }

        String safeError = error == null
                ? "Unknown publishing error"
                : error;

        // Avoid storing excessively large exception messages.
        if (safeError.length() > 2000) {
            safeError = safeError.substring(0, 2000);
        }

        int updated = outboxRepository.markFailureIfClaimed(
                eventId,
                claimToken,
                safeError,
                nextRetryAt
        );

        return updated == 1;
    }

}
