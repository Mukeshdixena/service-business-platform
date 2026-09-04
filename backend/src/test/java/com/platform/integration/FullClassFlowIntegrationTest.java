package com.platform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.platform.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end walk of the class lifecycle (CLAUDE_CODE.md §20):
 * create -> enroll -> waitlist once full -> roster -> attended/no-show, plus
 * customer self-cancel and the public discovery listing.
 */
class FullClassFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createEnrollWaitlistAndMarkAttendance() throws Exception {
        String ownerToken = registerAndGetAccessToken(uniqueEmail("yoga-owner"), "password123", "Yoga Owner", "BUSINESS_OWNER");

        JsonNode business = postJson("/api/v1/businesses", ownerToken, Map.of(
                "name", "Sunrise Yoga",
                "category", "ACADEMY",
                "capabilities", List.of("CLASSES")
        ), status().isCreated());
        String businessId = business.get("id").asText();
        postJson("/api/v1/businesses/" + businessId + "/publish", ownerToken, null, status().isOk());

        Instant startAt = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant endAt = startAt.plus(1, ChronoUnit.HOURS);

        // capacity of 1 so the second enrollment must waitlist
        JsonNode clazz = postJson("/api/v1/businesses/" + businessId + "/classes", ownerToken, Map.of(
                "name", "Morning Yoga",
                "description", "Gentle flow",
                "startAt", startAt.toString(),
                "endAt", endAt.toString(),
                "capacity", 1
        ), status().isCreated());
        String classId = clazz.get("id").asText();
        assertThat(clazz.get("status").asText()).isEqualTo("SCHEDULED");
        assertThat(clazz.get("enrolledCount").asLong()).isZero();

        String customer1Token = registerAndGetAccessToken(uniqueEmail("yc1"), "password123", "Customer One", "CUSTOMER");
        String customer2Token = registerAndGetAccessToken(uniqueEmail("yc2"), "password123", "Customer Two", "CUSTOMER");
        String customer3Token = registerAndGetAccessToken(uniqueEmail("yc3"), "password123", "Customer Three", "CUSTOMER");

        JsonNode enrollment1 = postJson("/api/v1/businesses/" + businessId + "/classes/" + classId + "/enroll",
                customer1Token, null, status().isCreated());
        assertThat(enrollment1.get("status").asText()).isEqualTo("ENROLLED");
        String enrollment1Id = enrollment1.get("id").asText();

        // class is now full -> waitlisted rather than rejected
        JsonNode enrollment2 = postJson("/api/v1/businesses/" + businessId + "/classes/" + classId + "/enroll",
                customer2Token, null, status().isCreated());
        assertThat(enrollment2.get("status").asText()).isEqualTo("WAITLISTED");

        // enrolling twice is rejected
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/classes/" + classId + "/enroll")
                        .header("Authorization", bearer(customer1Token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // derived enrolledCount counts only ENROLLED, not WAITLISTED
        mockMvc.perform(get("/api/v1/businesses/" + businessId + "/classes")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].enrolledCount").value(1))
                .andExpect(jsonPath("$.content[0].capacity").value(1));

        // owner reads the roster (both enrollments)
        mockMvc.perform(get("/api/v1/businesses/" + businessId + "/classes/" + classId + "/enrollments")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // customer 2 self-cancels their waitlisted place, freeing the waitlist seat
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/classes/" + classId + "/cancel-enrollment")
                        .header("Authorization", bearer(customer2Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // customer 3 arrives while the single seat is still taken -> waitlisted
        JsonNode enrollment3 = postJson("/api/v1/businesses/" + businessId + "/classes/" + classId + "/enroll",
                customer3Token, null, status().isCreated());
        assertThat(enrollment3.get("status").asText()).isEqualTo("WAITLISTED");
        String enrollment3Id = enrollment3.get("id").asText();

        // owner marks attendance outcomes
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/classes/" + classId
                        + "/enrollments/" + enrollment1Id + "/attended")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATTENDED"));
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/classes/" + classId
                        + "/enrollments/" + enrollment3Id + "/no-show")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_SHOW"));

        // customer sees their own enrollments across businesses
        mockMvc.perform(get("/api/v1/me/class-enrollments")
                        .header("Authorization", bearer(customer1Token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(enrollment1Id))
                .andExpect(jsonPath("$[0].status").value("ATTENDED"));

        // upcoming SCHEDULED classes appear on the public profile
        String slug = business.get("slug").asText();
        mockMvc.perform(get("/api/v1/discovery/businesses/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classes.length()").value(1))
                .andExpect(jsonPath("$.classes[0].name").value("Morning Yoga"))
                .andExpect(jsonPath("$.resources.length()").value(0));

        // soft delete cancels the class, which then rejects new enrollments and
        // drops out of the public upcoming listing
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/businesses/" + businessId + "/classes/" + classId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNoContent());

        String customer4Token = registerAndGetAccessToken(uniqueEmail("yc4"), "password123", "Customer Four", "CUSTOMER");
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/classes/" + classId + "/enroll")
                        .header("Authorization", bearer(customer4Token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/discovery/businesses/" + slug))
                .andExpect(jsonPath("$.classes.length()").value(0));
    }

    @Test
    void enrollingRequiresTheClassesCapability() throws Exception {
        String ownerToken = registerAndGetAccessToken(uniqueEmail("nc-owner"), "password123", "Owner", "BUSINESS_OWNER");
        JsonNode business = postJson("/api/v1/businesses", ownerToken, Map.of(
                "name", "No Classes Salon",
                "category", "SALON",
                "capabilities", List.of("APPOINTMENTS")
        ), status().isCreated());
        String businessId = business.get("id").asText();

        Instant startAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        JsonNode clazz = postJson("/api/v1/businesses/" + businessId + "/classes", ownerToken, Map.of(
                "name", "Ghost Class",
                "startAt", startAt.toString(),
                "endAt", startAt.plus(1, ChronoUnit.HOURS).toString(),
                "capacity", 5
        ), status().isCreated());

        String customerToken = registerAndGetAccessToken(uniqueEmail("nc-cust"), "password123", "Cust", "CUSTOMER");
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/classes/" + clazz.get("id").asText() + "/enroll")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void aClassMustEndAfterItStarts() throws Exception {
        String ownerToken = registerAndGetAccessToken(uniqueEmail("bad-owner"), "password123", "Owner", "BUSINESS_OWNER");
        JsonNode business = postJson("/api/v1/businesses", ownerToken, Map.of(
                "name", "Backwards Studio",
                "category", "GYM",
                "capabilities", List.of("CLASSES")
        ), status().isCreated());

        Instant startAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        mockMvc.perform(post("/api/v1/businesses/" + business.get("id").asText() + "/classes")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Time Traveller",
                                "startAt", startAt.toString(),
                                "endAt", startAt.minus(1, ChronoUnit.HOURS).toString(),
                                "capacity", 5))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
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
