package com.eventflow.outbox;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="event_outbox")
public class EventOutbox {

    @Id
    @Column(name = "outbox_id", nullable = false)
    private UUID outboxId;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name="next_retry_at")
    private Instant nextRetryAt;

    protected EventOutbox() {
        // Required by JPA
    }

    public EventOutbox(UUID eventId, String eventType, JsonNode payload) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = "PENDING";
        this.attempts = 0;
    }

    @PrePersist
    void prePersist() {
        if (outboxId == null) {
            outboxId = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = Instant.now();
        }

        if (status == null) {
            status = "PENDING";
        }
    }


    public UUID getOutboxId() {
        return outboxId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public String getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getNextRetryAt() {return nextRetryAt;}

    public void markPublished() {
        this.status = "PUBLISHED";
        this.publishedAt = Instant.now();
        this.lastError = null;
        this.nextRetryAt=null;
    }

//    public void markFailed(String error) {
//        this.status = "FAILED";
//        this.attempts++;
//        this.lastError = error;
//    }

    public void markRetryableFailure(String error) {
        this.attempts++;
        this.lastError = error;

        if (this.attempts >= 5) {
            this.status = "FAILED";
            this.nextRetryAt=null;
            return;
        }

        long delaySeconds = (long) Math.pow(2, this.attempts) * 10;

        this.nextRetryAt = Instant.now().plusSeconds(delaySeconds);

    }

    //replay method

    public void replay() {
        if (!"FAILED".equals(this.status)) {
            throw new IllegalStateException(
                    "Cannot replay event with status " + this.status
                            + "; only FAILED events can be replayed"
            );
        }

        this.status = "PENDING";
        this.attempts = 0;
        this.lastError = null;
        this.nextRetryAt = null;
        this.publishedAt = null;
    }



}
