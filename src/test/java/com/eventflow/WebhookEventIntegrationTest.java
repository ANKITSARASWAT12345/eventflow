//
//package com.eventflow;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
////import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.springframework.http.MediaType.APPLICATION_JSON;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
//class WebhookEventIntegrationTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Test
//    void createEventShouldReturn201() throws Exception {
//        String requestBody = """
//                {
//                  "eventType": "order.created",
//                  "payload": {
//                    "orderId": "ORD-101"
//                  }
//                }
//                """;
//
//        mockMvc.perform(post("/api/v1/events")
//                        .contentType(APPLICATION_JSON)
//                        .content(requestBody))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.eventId").exists());
//    }
//
//    @Test
//    void missingEventShouldReturn404() throws Exception {
//        mockMvc.perform(get(
//                        "/api/v1/events/00000000-0000-0000-0000-000000000000"))
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.status").value(404))
//                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
//    }
//}



package com.eventflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
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

    @Test
    void createEventShouldReturn201() throws Exception {
        String requestBody = """
                {
                  "eventType": "order.created",
                  "payload": {
                    "orderId": "ORD-101"
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").exists());
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
