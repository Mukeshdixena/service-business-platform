package com.platform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.platform.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end walk of attendance + capacity (CLAUDE_CODE.md §18-19):
 * check-in -> occupancy rises -> duplicate check-in rejected -> check-out ->
 * occupancy falls. Occupancy is asserted through the API precisely because it
 * must be derived from open attendance rows, not from a stored counter.
 */
class FullAttendanceFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void checkInAndCheckOutDriveDerivedOccupancy() throws Exception {
        String ownerToken = registerAndGetAccessToken(uniqueEmail("gym-owner"), "password123", "Gym Owner", "BUSINESS_OWNER");

        JsonNode business = postJson("/api/v1/businesses", ownerToken, Map.of(
                "name", "Iron Gym",
                "category", "GYM",
                "capabilities", List.of("CAPACITY", "MEMBERSHIPS"),
                "maxCapacity", 3
        ), status().isCreated());
        String businessId = business.get("id").asText();
        assertThat(business.get("maxCapacity").asInt()).isEqualTo(3);

        // empty gym
        mockMvc.perform(get("/api/v1/businesses/" + businessId + "/capacity")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(0))
                .andExpect(jsonPath("$.capacity").value(3))
                .andExpect(jsonPath("$.available").value(3));

        String memberAToken = registerAndGetAccessToken(uniqueEmail("member-a"), "password123", "Member A", "CUSTOMER");
        String memberBToken = registerAndGetAccessToken(uniqueEmail("member-b"), "password123", "Member B", "CUSTOMER");
        String memberAId = customerProfileId(memberAToken);
        String memberBId = customerProfileId(memberBToken);

        JsonNode attendanceA = postJson("/api/v1/businesses/" + businessId + "/attendance/check-in", ownerToken,
                Map.of("customerId", memberAId), status().isCreated());
        // null fields are omitted globally (spring.jackson.default-property-inclusion=non_null)
        assertThat(attendanceA.hasNonNull("checkOutAt")).isFalse();
        String attendanceAId = attendanceA.get("id").asText();

        postJson("/api/v1/businesses/" + businessId + "/attendance/check-in", ownerToken,
                Map.of("customerId", memberBId), status().isCreated());

        mockMvc.perform(get("/api/v1/businesses/" + businessId + "/capacity")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(jsonPath("$.current").value(2))
                .andExpect(jsonPath("$.available").value(1));

        // a second concurrently-open record for the same customer is rejected
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/attendance/check-in")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("customerId", memberAId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ATTENDANCE_CONFLICT"));

        // activeOnly listing shows both open visits
        mockMvc.perform(get("/api/v1/businesses/" + businessId + "/attendance?activeOnly=true")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // check A out
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/attendance/" + attendanceAId + "/check-out")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkOutAt").isNotEmpty());

        // checking the same record out twice is a conflict
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/attendance/" + attendanceAId + "/check-out")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ATTENDANCE_CONFLICT"));

        mockMvc.perform(get("/api/v1/businesses/" + businessId + "/capacity")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(jsonPath("$.current").value(1))
                .andExpect(jsonPath("$.available").value(2));

        // A left, so A can check in again
        postJson("/api/v1/businesses/" + businessId + "/attendance/check-in", ownerToken,
                Map.of("customerId", memberAId), status().isCreated());

        // full history (activeOnly=false) keeps the closed visit
        mockMvc.perform(get("/api/v1/businesses/" + businessId + "/attendance")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void capacityIsNullWhenMaxCapacityIsUnsetAndSettableLater() throws Exception {
        String ownerToken = registerAndGetAccessToken(uniqueEmail("cap-owner"), "password123", "Cap Owner", "BUSINESS_OWNER");
        JsonNode business = postJson("/api/v1/businesses", ownerToken, Map.of(
                "name", "Uncapped Studio",
                "category", "GYM",
                "capabilities", List.of("CAPACITY")
        ), status().isCreated());
        String businessId = business.get("id").asText();

        mockMvc.perform(get("/api/v1/businesses/" + businessId + "/capacity")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(0))
                .andExpect(jsonPath("$.capacity").doesNotExist())
                .andExpect(jsonPath("$.available").doesNotExist());

        mockMvc.perform(patch("/api/v1/businesses/" + businessId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("maxCapacity", 50))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxCapacity").value(50));

        mockMvc.perform(get("/api/v1/businesses/" + businessId + "/capacity")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(jsonPath("$.capacity").value(50))
                .andExpect(jsonPath("$.available").value(50));

        // a non-positive maxCapacity is rejected by validation
        mockMvc.perform(patch("/api/v1/businesses/" + businessId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("maxCapacity", 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private String customerProfileId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/users/me/customer-profile")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private JsonNode postJson(String url, String token, Map<String, ?> body, ResultMatcher expectedStatus) throws Exception {
        var requestBuilder = post(url).header("Authorization", bearer(token));
        if (body != null) {
            requestBuilder = requestBuilder.contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body));
        }
        MvcResult result = mockMvc.perform(requestBuilder).andExpect(expectedStatus).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
