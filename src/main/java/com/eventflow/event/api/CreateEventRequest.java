package com.eventflow.event.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

public record CreateEventRequest(

        @NotNull
        @Size(max=120)
        String eventType,

        @NotNull
        JsonNode payload
) {
}
