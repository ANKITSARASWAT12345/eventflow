package com.eventflow.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class OutboxPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public OutboxPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${eventflow.kafka.topic:eventflow.events}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public void publish(EventOutbox outbox) {

        ObjectNode message = objectMapper.createObjectNode();

        message.put("eventId", outbox.getEventId().toString());
        message.put("eventType", outbox.getEventType());
        message.set("payload", outbox.getPayload());

        String messageJson = objectMapper.writeValueAsString(message);

        try {
            kafkaTemplate.send(
                    topic,
                    outbox.getEventId().toString(),
                    messageJson
            ).get(10, TimeUnit.SECONDS);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Kafka publishing was interrupted", ex
            );

        } catch (ExecutionException | TimeoutException ex) {
            throw new IllegalStateException(
                    "Failed to publish event to Kafka: "
                            + outbox.getEventId(), ex
            );
        }
    }
}
