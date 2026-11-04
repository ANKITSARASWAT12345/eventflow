package com.eventflow.event.application;

import com.eventflow.common.exception.EventNotFoundException;
import com.eventflow.event.api.CreateEventRequest;
import com.eventflow.event.api.EventResponse;
import com.eventflow.event.domain.WebhookEvent;
import com.eventflow.event.persistence.WebhookEventRepository;
import com.eventflow.outbox.EventOutbox;
import com.eventflow.outbox.EventOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EventService {

    private final WebhookEventRepository webhookEventRepository;
    private final EventOutboxRepository eventOutboxRepository;

    public EventService(WebhookEventRepository webhookEventRepository, EventOutboxRepository eventOutboxRepository){
        this.webhookEventRepository=webhookEventRepository;
        this.eventOutboxRepository=eventOutboxRepository;
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

        EventOutbox outbox = new EventOutbox(
                saved.getEventId(),
                saved.getEventType(),
                saved.getPayload()
        );
        eventOutboxRepository.save(outbox);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)

    public EventResponse getEvent(UUID eventId) {
        WebhookEvent event = webhookEventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        return toResponse(event);

    }

    @Transactional
    public void replayEvent(UUID eventId){

        EventOutbox outbox = eventOutboxRepository.findByEventId(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        outbox.replay();

        eventOutboxRepository.save(outbox);

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
