
package com.eventflow;

import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
