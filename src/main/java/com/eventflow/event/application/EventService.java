package com.eventflow.event.application;

import com.eventflow.common.exception.EventNotFoundException;
import com.eventflow.event.api.CreateEventRequest;
import com.eventflow.event.api.EventResponse;
import com.eventflow.event.domain.WebhookEvent;
import com.eventflow.event.persistence.WebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EventService {

    private final WebhookEventRepository webhookEventRepository;

    public EventService(WebhookEventRepository webhookEventRepository){
        this.webhookEventRepository=webhookEventRepository;
    }

    @Transactional
    public EventResponse createEvent(CreateEventRequest request){
        if (!request.payload().isObject()) {
            throw new IllegalArgumentException(
                    "Event payload must be a JSON object"
            );
        }

        WebhookEvent event = new WebhookEvent(
                request.eventType(),
                request.payload()
        );

        WebhookEvent saved = webhookEventRepository.saveAndFlush(event);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)

    public EventResponse getEvent(UUID eventId) {
        WebhookEvent event = webhookEventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        return toResponse(event);

    }


    private EventResponse toResponse(WebhookEvent event) {
        return new EventResponse(
                event.getEventId(),
                event.getEventType(),
                event.getPayload(),
                event.getStatus(),
                event.getCreatedAt()
        );
    }


}
