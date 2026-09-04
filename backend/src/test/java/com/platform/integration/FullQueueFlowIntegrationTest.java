package com.platform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.platform.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end walk of the queue lifecycle (CLAUDE_CODE.md §15-16, §35):
 * join -> call -> start -> complete, plus the duplicate-active-entry rejection
 * and an invalid-transition rejection.
 */
class FullQueueFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void customerJoinsQueueAndOwnerAdvancesItToCompletion() throws Exception {
        String ownerToken = registerAndGetAccessToken(uniqueEmail("owner"), "password123", "Priya Owner", "BUSINESS_OWNER");

        JsonNode business = postJson("/api/v1/businesses", ownerToken, Map.of(
                "name", "Walkin Salon",
                "category", "SALON",
                "capabilities", List.of("QUEUE")
        ), status().isCreated());
        String businessId = business.get("id").asText();

        JsonNode service = postJson("/api/v1/businesses/" + businessId + "/services", ownerToken, Map.of(
                "name", "Quick Trim",
                "price", 100.00,
                "currency", "INR",
                "durationMinutes", 10,
                "bookingType", "WALK_IN"
        ), status().isCreated());
        String serviceId = service.get("id").asText();

        postJson("/api/v1/businesses/" + businessId + "/publish", ownerToken, null, status().isOk());

        String customer1Token = registerAndGetAccessToken(uniqueEmail("cust1"), "password123", "Customer One", "CUSTOMER");
        String customer2Token = registerAndGetAccessToken(uniqueEmail("cust2"), "password123", "Customer Two", "CUSTOMER");

        // customer 1 joins -> position 1
        JsonNode entry1 = postJson("/api/v1/businesses/" + businessId + "/queue", customer1Token, Map.of(
                "serviceId", serviceId
        ), status().isCreated());
        String entry1Id = entry1.get("id").asText();
        assertThat(entry1.get("status").asText()).isEqualTo("WAITING");
        assertThat(entry1.get("position").asInt()).isEqualTo(1);
        assertThat(entry1.get("estimatedWaitMinutes").asInt()).isEqualTo(0);

        // customer 1 tries to join again -> QUEUE_CONFLICT
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/queue")
                        .header("Authorization", bearer(customer1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("serviceId", serviceId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUEUE_CONFLICT"));

        // customer 2 joins -> position 2, one person ahead
        JsonNode entry2 = postJson("/api/v1/businesses/" + businessId + "/queue", customer2Token, Map.of(
                "serviceId", serviceId
        ), status().isCreated());
        String entry2Id = entry2.get("id").asText();
        assertThat(entry2.get("position").asInt()).isEqualTo(2);
        assertThat(entry2.get("estimatedWaitMinutes").asInt()).isEqualTo(10);

        // owner lists the queue, ordered by position
        mockMvc.perform(get("/api/v1/businesses/" + businessId + "/queue")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(entry1Id))
                .andExpect(jsonPath("$[1].id").value(entry2Id));

        // owner cannot skip WAITING straight to SERVING (invalid transition)
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/queue/" + entry1Id + "/start")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));

        // owner walks entry 1 through its lifecycle
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/queue/" + entry1Id + "/call")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CALLED"));
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/queue/" + entry1Id + "/start")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SERVING"));
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/queue/" + entry1Id + "/complete")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // now that entry1 is out of the active line, customer1 can join again (no longer "active")
        postJson("/api/v1/businesses/" + businessId + "/queue", customer1Token, Map.of(
                "serviceId", serviceId
        ), status().isCreated());

        // customer 2 checks their own status: now first in line
        MvcResult statusResult = mockMvc.perform(get("/api/v1/me/queue-entries/" + entry2Id + "/status")
                        .header("Authorization", bearer(customer2Token)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode statusJson = objectMapper.readTree(statusResult.getResponse().getContentAsString());
        assertThat(statusJson.get("status").asText()).isEqualTo("WAITING");
        assertThat(statusJson.get("peopleAhead").asInt()).isEqualTo(0);

        // customer 2 sees their own history
        mockMvc.perform(get("/api/v1/me/queue-entries")
                        .header("Authorization", bearer(customer2Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(entry2Id));

        // customer 2 self-cancels
        mockMvc.perform(post("/api/v1/me/queue-entries/" + entry2Id + "/cancel")
                        .header("Authorization", bearer(customer2Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void joiningRequiresTheQueueCapability() throws Exception {
        String ownerToken = registerAndGetAccessToken(uniqueEmail("owner2"), "password123", "Owner Two", "BUSINESS_OWNER");
        JsonNode business = postJson("/api/v1/businesses", ownerToken, Map.of(
                "name", "No Queue Salon",
                "category", "SALON",
                "capabilities", List.of("APPOINTMENTS")
        ), status().isCreated());
        String businessId = business.get("id").asText();

        JsonNode service = postJson("/api/v1/businesses/" + businessId + "/services", ownerToken, Map.of(
                "name", "Cut",
                "price", 50.00,
                "currency", "INR",
                "durationMinutes", 15,
                "bookingType", "WALK_IN"
        ), status().isCreated());
        postJson("/api/v1/businesses/" + businessId + "/publish", ownerToken, null, status().isOk());

        String customerToken = registerAndGetAccessToken(uniqueEmail("cust3"), "password123", "Customer Three", "CUSTOMER");

        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/queue")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("serviceId", service.get("id").asText()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private JsonNode postJson(String url, String token, Map<String, ?> body, org.springframework.test.web.servlet.ResultMatcher expectedStatus) throws Exception {
        var requestBuilder = post(url).header("Authorization", bearer(token));
        if (body != null) {
            requestBuilder = requestBuilder.contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body));
        }
        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(expectedStatus)
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
