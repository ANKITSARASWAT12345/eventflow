
package com.eventflow.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.UUID;

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
    private final OutboxCompletionService completionService;
    private final OutboxPublisher outboxPublisher;
    private final int batchSize;

    public OutboxWorker(
            OutboxClaimService claimService,
            OutboxCompletionService completionService,
            OutboxPublisher outboxPublisher,
            @Value("${eventflow.outbox.batch-size:100}") int batchSize
    ) {
        this.claimService = claimService;
        this.completionService = completionService;
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

            UUID eventId = outbox.getEventId();
            UUID claimToken = outbox.getClaimToken();
            int currentAttempts = outbox.getAttempts();

            try {
                outboxPublisher.publish(outbox);

            } catch (Exception ex) {

                boolean recorded = completionService.markFailure(
                        eventId,
                        claimToken,
                        currentAttempts,
                        ex.getMessage()
                );

                if (recorded) {
                    log.error(
                            "Failed to publish event. eventId={}, attempts={}",
                            eventId,
                            currentAttempts + 1,
                            ex
                    );
                } else {
                    log.warn(
                            "Failure update skipped because claim is no longer valid. eventId={}",
                            eventId
                    );
                }

                continue;
            }

            boolean completed = completionService.markPublished(
                    eventId,
                    claimToken
            );

            if (completed) {
                log.info(
                        "Event published successfully. eventId={}",
                        eventId
                );
            } else {
                log.warn(
                        "Publish completion skipped because claim is no longer valid. eventId={}",
                        eventId
                );
            }
        }
    }
}
