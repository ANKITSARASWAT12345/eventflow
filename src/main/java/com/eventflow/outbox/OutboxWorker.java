package com.eventflow.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Component
@ConditionalOnProperty(
        name = "eventflow.outbox.worker.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxWorker {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxWorker.class);

    private final OutboxClaimService claimService;
    private final EventOutboxRepository outboxRepository;
    private final OutboxPublisher outboxPublisher;
    private final int batchSize;

    public OutboxWorker(
            OutboxClaimService claimService,
            EventOutboxRepository outboxRepository,
            OutboxPublisher outboxPublisher,
            @Value("${eventflow.outbox.batch-size:100}") int batchSize
    ) {
        this.claimService = claimService;
        this.outboxRepository = outboxRepository;
        this.outboxPublisher = outboxPublisher;
        this.batchSize = batchSize;
    }

    @Scheduled(
            fixedDelayString = "${eventflow.outbox.poll-interval-ms:5000}"
    )
    public void processPendingEvents() {

        // Recover events abandoned by crashed workers.
        int recovered = claimService.recoverExpiredClaims();

        if (recovered > 0) {
            log.warn("Recovered {} expired outbox claims", recovered);
        }

        // Claim a batch of pending events.
        List<EventOutbox> claimedEvents =
                claimService.claimPendingEvents(batchSize);

        for (EventOutbox outbox : claimedEvents) {

            try {
                outboxPublisher.publish(outbox);

                outbox.markPublished();
                outboxRepository.save(outbox);

                log.info(
                        "Event published successfully. eventId={}",
                        outbox.getEventId()
                );

            } catch (Exception ex) {

                outbox.markRetryableFailure(ex.getMessage());
                outboxRepository.save(outbox);

                log.error(
                        "Failed to publish event. eventId={}, attempts={}, nextRetryAt={}, status={}",
                        outbox.getEventId(),
                        outbox.getAttempts(),
                        outbox.getNextRetryAt(),
                        outbox.getStatus(),
                        ex
                );
            }
        }
    }
}
