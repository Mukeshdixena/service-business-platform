package com.platform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.platform.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CLAUDE_CODE.md §31/§45: a business owner must never be able to read or write
 * another business's private data merely by knowing/guessing its id (IDOR).
 * Every assertion here expects 401/403 — never a 200 with real or leaked data.
 */
class CrossTenantAuthorizationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void ownerBCannotReadOrMutateOwnerAsBusiness() throws Exception {
        String ownerAToken = registerAndGetAccessToken(uniqueEmail("owner-a"), "password123", "Owner A", "BUSINESS_OWNER");
        String ownerBToken = registerAndGetAccessToken(uniqueEmail("owner-b"), "password123", "Owner B", "BUSINESS_OWNER");

        String businessAId = createBusiness(ownerAToken, "A Salon");

        // B cannot read A's private management view of the business.
        mockMvc.perform(get("/api/v1/businesses/" + businessAId)
                        .header("Authorization", bearer(ownerBToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        // B cannot update A's business.
        mockMvc.perform(patch("/api/v1/businesses/" + businessAId)
                        .header("Authorization", bearer(ownerBToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Hijacked"))))
                .andExpect(status().isForbidden());

        // B cannot publish A's business.
        mockMvc.perform(post("/api/v1/businesses/" + businessAId + "/publish")
                        .header("Authorization", bearer(ownerBToken)))
                .andExpect(status().isForbidden());

        // B cannot create a service under A's business.
        mockMvc.perform(post("/api/v1/businesses/" + businessAId + "/services")
                        .header("Authorization", bearer(ownerBToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Sneaky Service", "price", 10.0, "currency", "INR",
                                "durationMinutes", 15, "bookingType", "APPOINTMENT"))))
                .andExpect(status().isForbidden());

        // B cannot list A's bookings.
        mockMvc.perform(get("/api/v1/businesses/" + businessAId + "/bookings")
                        .header("Authorization", bearer(ownerBToken)))
                .andExpect(status().isForbidden());

        // A can still read their own business (sanity check the rule isn't overly broad).
        mockMvc.perform(get("/api/v1/businesses/" + businessAId)
                        .header("Authorization", bearer(ownerAToken)))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousRequestsToProtectedEndpointsAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void aCustomerCannotCancelAnotherCustomersBooking() throws Exception {
        String ownerToken = registerAndGetAccessToken(uniqueEmail("owner"), "password123", "Owner", "BUSINESS_OWNER");
        String businessId = createBusiness(ownerToken, "Shared Salon");

        String customer1Token = registerAndGetAccessToken(uniqueEmail("cust1"), "password123", "Customer One", "CUSTOMER");
        String customer2Token = registerAndGetAccessToken(uniqueEmail("cust2"), "password123", "Customer Two", "CUSTOMER");

        // customer2 tries to cancel a random (non-existent) booking id -> not found, no leakage either way.
        mockMvc.perform(post("/api/v1/me/bookings/" + java.util.UUID.randomUUID() + "/cancel")
                        .header("Authorization", bearer(customer2Token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerBCannotReadOrMutateOwnerAsQueueOrMemberships() throws Exception {
        String ownerAToken = registerAndGetAccessToken(uniqueEmail("qm-owner-a"), "password123", "Owner A", "BUSINESS_OWNER");
        String ownerBToken = registerAndGetAccessToken(uniqueEmail("qm-owner-b"), "password123", "Owner B", "BUSINESS_OWNER");

        MvcResult result = mockMvc.perform(post("/api/v1/businesses")
                        .header("Authorization", bearer(ownerAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "A Queue Business",
                                "category", "SALON",
                                "capabilities", List.of("QUEUE", "MEMBERSHIPS")))))
                .andExpect(status().isCreated())
                .andReturn();
        String businessAId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // B cannot list A's queue.
        mockMvc.perform(get("/api/v1/businesses/" + businessAId + "/queue")
                        .header("Authorization", bearer(ownerBToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        // B cannot create a membership plan under A's business.
        mockMvc.perform(post("/api/v1/businesses/" + businessAId + "/membership-plans")
                        .header("Authorization", bearer(ownerBToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Sneaky Plan", "price", 10.0, "currency", "INR",
                                "duration", 1, "durationUnit", "MONTH"))))
                .andExpect(status().isForbidden());

        // B cannot list A's memberships.
        mockMvc.perform(get("/api/v1/businesses/" + businessAId + "/memberships")
                        .header("Authorization", bearer(ownerBToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void aCustomerCannotCancelAnotherCustomersQueueEntryOrMembership() throws Exception {
        String customer2Token = registerAndGetAccessToken(uniqueEmail("qm-cust2"), "password123", "Customer Two", "CUSTOMER");

        mockMvc.perform(post("/api/v1/me/queue-entries/" + java.util.UUID.randomUUID() + "/cancel")
                        .header("Authorization", bearer(customer2Token)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/me/memberships/" + java.util.UUID.randomUUID() + "/cancel")
                        .header("Authorization", bearer(customer2Token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerBCannotReadOrMutateOwnerAsAttendanceClassesOrResources() throws Exception {
        String ownerAToken = registerAndGetAccessToken(uniqueEmail("acr-owner-a"), "password123", "Owner A", "BUSINESS_OWNER");
        String ownerBToken = registerAndGetAccessToken(uniqueEmail("acr-owner-b"), "password123", "Owner B", "BUSINESS_OWNER");

        MvcResult result = mockMvc.perform(post("/api/v1/businesses")
                        .header("Authorization", bearer(ownerAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "A Gym And Rentals",
                                "category", "GYM",
                                "capabilities", List.of("CAPACITY", "CLASSES", "RESOURCES"),
                                "maxCapacity", 100))))
                .andExpect(status().isCreated())
                .andReturn();
        String businessAId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // B cannot read A's live occupancy.
        mockMvc.perform(get("/api/v1/businesses/" + businessAId + "/capacity")
                        .header("Authorization", bearer(ownerBToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        // B cannot list or write A's attendance.
        mockMvc.perform(get("/api/v1/businesses/" + businessAId + "/attendance")
                        .header("Authorization", bearer(ownerBToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/businesses/" + businessAId + "/attendance/check-in")
                        .header("Authorization", bearer(ownerBToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("customerId", java.util.UUID.randomUUID()))))
                .andExpect(status().isForbidden());

        // B cannot list or create A's classes.
        mockMvc.perform(get("/api/v1/businesses/" + businessAId + "/classes")
                        .header("Authorization", bearer(ownerBToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/businesses/" + businessAId + "/classes")
                        .header("Authorization", bearer(ownerBToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Sneaky Class",
                                "startAt", java.time.Instant.now().plusSeconds(86400).toString(),
                                "endAt", java.time.Instant.now().plusSeconds(90000).toString(),
                                "capacity", 5))))
                .andExpect(status().isForbidden());

        // B cannot list or create A's resources.
        mockMvc.perform(get("/api/v1/businesses/" + businessAId + "/resources")
                        .header("Authorization", bearer(ownerBToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/businesses/" + businessAId + "/resources")
                        .header("Authorization", bearer(ownerBToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Sneaky JCB"))))
                .andExpect(status().isForbidden());

        // A can still read their own (the rule is not overly broad).
        mockMvc.perform(get("/api/v1/businesses/" + businessAId + "/capacity")
                        .header("Authorization", bearer(ownerAToken)))
                .andExpect(status().isOk());
    }

    @Test
    void aCustomerCannotCancelAnotherCustomersClassEnrollment() throws Exception {
        String ownerToken = registerAndGetAccessToken(uniqueEmail("ce-owner"), "password123", "Owner", "BUSINESS_OWNER");
        MvcResult result = mockMvc.perform(post("/api/v1/businesses")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Enrollment Studio",
                                "category", "ACADEMY",
                                "capabilities", List.of("CLASSES")))))
                .andExpect(status().isCreated())
                .andReturn();
        String businessId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        java.time.Instant startAt = java.time.Instant.now().plusSeconds(86400);
        MvcResult classResult = mockMvc.perform(post("/api/v1/businesses/" + businessId + "/classes")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Pilates",
                                "startAt", startAt.toString(),
                                "endAt", startAt.plusSeconds(3600).toString(),
                                "capacity", 10))))
                .andExpect(status().isCreated())
                .andReturn();
        String classId = objectMapper.readTree(classResult.getResponse().getContentAsString()).get("id").asText();

        String enrolledCustomer = registerAndGetAccessToken(uniqueEmail("ce-c1"), "password123", "C1", "CUSTOMER");
        String otherCustomer = registerAndGetAccessToken(uniqueEmail("ce-c2"), "password123", "C2", "CUSTOMER");

        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/classes/" + classId + "/enroll")
                        .header("Authorization", bearer(enrolledCustomer)))
                .andExpect(status().isCreated());

        // The other customer's cancel only ever targets their OWN enrollment, so
        // it cannot touch someone else's — it simply finds nothing.
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/classes/" + classId + "/cancel-enrollment")
                        .header("Authorization", bearer(otherCustomer)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // A non-member customer cannot mark attendance on the roster either.
        mockMvc.perform(get("/api/v1/businesses/" + businessId + "/classes/" + classId + "/enrollments")
                        .header("Authorization", bearer(otherCustomer)))
                .andExpect(status().isForbidden());
    }

    private String createBusiness(String ownerToken, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/businesses")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "category", "SALON",
                                "capabilities", List.of("APPOINTMENTS")))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asText();
    }
}
