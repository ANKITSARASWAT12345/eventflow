package com.eventflow.event.api;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record EventResponse(
        UUID eventId,
        String eventType,
        JsonNode payload,
        String status,
        Instant createdAt

) {
}
