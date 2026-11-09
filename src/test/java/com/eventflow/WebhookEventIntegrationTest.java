
package com.eventflow;

import com.eventflow.outbox.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "eventflow.outbox.worker.enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers
class WebhookEventIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("eventflow_test")
                    .withUsername("eventflow")
                    .withPassword("test_password");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OutboxClaimService claimService;

    @Autowired
    private OutboxCompletionService completionService;

    @Test
    void createEventShouldSaveEventAndOutboxRecord() throws Exception {

        String requestBody = """
                {
                  "eventType": "order.created",
                  "payload": {
                    "orderId": "ORD-101"
                  }
                }
                """;

        String response = mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String eventId = com.jayway.jsonpath.JsonPath.read(
                response, "$.eventId"
        );

        Integer eventCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM webhook_events WHERE event_id = ?",
                Integer.class,
                java.util.UUID.fromString(eventId)
        );

        Integer outboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event_outbox WHERE event_id = ?",
                Integer.class,
                java.util.UUID.fromString(eventId)
        );

        String outboxStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM event_outbox WHERE event_id = ?",
                String.class,
                java.util.UUID.fromString(eventId)
        );

        assertEquals(1, eventCount,
                "Exactly one event should be saved");

        assertEquals(1, outboxCount,
                "Exactly one outbox record should be saved");

        assertEquals("PENDING", outboxStatus,
                "New outbox record should initially be PENDING");
    }

    @Test
    void missingEventShouldReturn404() throws Exception {

        mockMvc.perform(get(
                        "/api/v1/events/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void failedPublishingShouldScheduleRetry() throws Exception {

        // 1. Create an event through the actual API.
        String requestBody = """
            {
              "eventType": "order.created",
              "payload": {
                "orderId": "ORD-RETRY-101"
              }
            }
            """;

        String response = mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String eventId = com.jayway.jsonpath.JsonPath.read(
                response, "$.eventId"
        );

        UUID eventUUID = UUID.fromString(eventId);

        // 2. Mock Kafka publisher to simulate publishing failure.
        OutboxPublisher publisher = Mockito.mock(OutboxPublisher.class);

        Mockito.doThrow(
                new IllegalStateException("Simulated Kafka failure")
        ).when(publisher).publish(any(EventOutbox.class));

        // 3. Create worker using real database services.
        OutboxWorker worker = new OutboxWorker(
                claimService,
                completionService,
                publisher,
                10
        );

        // 4. Process the pending event.
        worker.processPendingEvents();

        // 5. Verify database state after failure.
        String outboxStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM event_outbox WHERE event_id = ?",
                String.class,
                eventUUID
        );

        Integer attempts = jdbcTemplate.queryForObject(
                "SELECT attempts FROM event_outbox WHERE event_id = ?",
                Integer.class,
                eventUUID
        );

        String lastError = jdbcTemplate.queryForObject(
                "SELECT last_error FROM event_outbox WHERE event_id = ?",
                String.class,
                eventUUID
        );

        Boolean retryScheduled = jdbcTemplate.queryForObject(
                """
                SELECT next_retry_at > CURRENT_TIMESTAMP
                FROM event_outbox
                WHERE event_id = ?
                """,
                Boolean.class,
                eventUUID
        );

        Boolean claimReleased = jdbcTemplate.queryForObject(
                """
                SELECT locked_at IS NULL AND claim_token IS NULL
                FROM event_outbox
                WHERE event_id = ?
                """,
                Boolean.class,
                eventUUID
        );

        assertEquals("PENDING", outboxStatus,
                "Failed event should return to PENDING");

        assertEquals(1, attempts,
                "Attempts should increment to 1");

        assertEquals("Simulated Kafka failure", lastError,
                "Last error should be recorded");

        assertEquals(Boolean.TRUE, retryScheduled,
                "Next retry time should be in the future");

        assertEquals(Boolean.TRUE, claimReleased,
                "Claim should be released after failure");
    }


}
