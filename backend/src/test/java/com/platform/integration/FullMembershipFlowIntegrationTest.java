package com.platform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.platform.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end walk of the membership lifecycle (CLAUDE_CODE.md §17):
 * create plan -> purchase -> freeze -> reactivate -> cancel.
 */
class FullMembershipFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void ownerCreatesAPlanAndACustomerPurchasesAndTheOwnerManagesIt() throws Exception {
        String ownerToken = registerAndGetAccessToken(uniqueEmail("gymowner"), "password123", "Gym Owner", "BUSINESS_OWNER");

        JsonNode business = postJson("/api/v1/businesses", ownerToken, Map.of(
                "name", "Iron Gym",
                "category", "GYM",
                "capabilities", List.of("MEMBERSHIPS")
        ), status().isCreated());
        String businessId = business.get("id").asText();
        postJson("/api/v1/businesses/" + businessId + "/publish", ownerToken, null, status().isOk());

        JsonNode plan = postJson("/api/v1/businesses/" + businessId + "/membership-plans", ownerToken, Map.of(
                "name", "Monthly",
                "description", "One month unlimited access",
                "price", 1500.00,
                "currency", "INR",
                "duration", 1,
                "durationUnit", "MONTH"
        ), status().isCreated());
        String planId = plan.get("id").asText();
        assertThat(plan.get("status").asText()).isEqualTo("ACTIVE");

        // the plan is browsable publicly via discovery (BusinessPublicDto.membershipPlans),
        // since GET /membership-plans is OWNER/STAFF-only management access.
        String slug = business.get("slug").asText();
        mockMvc.perform(get("/api/v1/discovery/businesses/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipPlans[0].id").value(planId))
                .andExpect(jsonPath("$.membershipPlans[0].status").value("ACTIVE"));

        String customerToken = registerAndGetAccessToken(uniqueEmail("member"), "password123", "Gym Member", "CUSTOMER");

        JsonNode membership = postJson("/api/v1/businesses/" + businessId + "/memberships", customerToken, Map.of(
                "membershipPlanId", planId
        ), status().isCreated());
        String membershipId = membership.get("id").asText();
        assertThat(membership.get("status").asText()).isEqualTo("ACTIVE");
        // paymentId is always null this phase; the app's global Jackson config
        // (default-property-inclusion: non_null) omits null fields entirely.
        assertThat(membership.has("paymentId")).isFalse();
        assertThat(LocalDate.parse(membership.get("startDate").asText())).isEqualTo(LocalDate.now());
        assertThat(LocalDate.parse(membership.get("endDate").asText())).isEqualTo(LocalDate.now().plusMonths(1));

        // owner sees it in the business's membership list
        mockMvc.perform(get("/api/v1/businesses/" + businessId + "/memberships")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(membershipId));

        // owner freezes it
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/memberships/" + membershipId + "/freeze")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));

        // cannot cancel-then-freeze-again style invalid jump: FROZEN -> EXPIRED is not allowed
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/memberships/" + membershipId + "/freeze")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));

        // owner reactivates it
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/memberships/" + membershipId + "/reactivate")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // customer sees their own membership
        MvcResult myMembershipsResult = mockMvc.perform(get("/api/v1/me/memberships")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode myMemberships = objectMapper.readTree(myMembershipsResult.getResponse().getContentAsString());
        assertThat(myMemberships.get(0).get("id").asText()).isEqualTo(membershipId);

        // owner cancels it
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/memberships/" + membershipId + "/cancel")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // a cancelled membership cannot be reactivated
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/memberships/" + membershipId + "/reactivate")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void purchasingRequiresTheMembershipsCapabilityAndAnActivePlan() throws Exception {
        String ownerToken = registerAndGetAccessToken(uniqueEmail("gymowner2"), "password123", "Gym Owner Two", "BUSINESS_OWNER");
        JsonNode business = postJson("/api/v1/businesses", ownerToken, Map.of(
                "name", "No Membership Gym",
                "category", "GYM",
                "capabilities", List.of("STAFF")
        ), status().isCreated());
        String businessId = business.get("id").asText();
        postJson("/api/v1/businesses/" + businessId + "/publish", ownerToken, null, status().isOk());

        // no MEMBERSHIPS capability -> discovery never surfaces plans, empty array not null.
        mockMvc.perform(get("/api/v1/discovery/businesses/" + business.get("slug").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipPlans").isArray())
                .andExpect(jsonPath("$.membershipPlans").isEmpty());

        String customerToken = registerAndGetAccessToken(uniqueEmail("member2"), "password123", "Member Two", "CUSTOMER");

        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/memberships")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("membershipPlanId", java.util.UUID.randomUUID().toString()))))
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
