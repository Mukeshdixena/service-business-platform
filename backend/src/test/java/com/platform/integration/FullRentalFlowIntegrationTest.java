package com.platform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.platform.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end walk of a rental (CLAUDE_CODE.md §11, §34, Phase 7): resource CRUD,
 * rental booking with server-computed price, the §34 overlap rejection, and the
 * booking lifecycle through to COMPLETED. Also covers the RENTAL/APPOINTMENT
 * field rules and the MAINTENANCE guard.
 */
class FullRentalFlowIntegrationTest extends AbstractIntegrationTest {

    private static final Instant RENTAL_START = LocalDate.now(ZoneOffset.UTC).plusDays(7)
            .atTime(9, 0).toInstant(ZoneOffset.UTC);

    @Test
    void reserveARentalRejectAConflictAndCompleteIt() throws Exception {
        String ownerToken = registerAndGetAccessToken(uniqueEmail("jcb-owner"), "password123", "JCB Owner", "BUSINESS_OWNER");

        JsonNode business = postJson("/api/v1/businesses", ownerToken, Map.of(
                "name", "Mahadev JCB Rentals",
                "category", "EQUIPMENT_RENTAL",
                "capabilities", List.of("RENTALS", "RESOURCES")
        ), status().isCreated());
        String businessId = business.get("id").asText();

        // hourly rental service at 500/hour
        JsonNode service = postJson("/api/v1/businesses/" + businessId + "/services", ownerToken, Map.of(
                "name", "JCB Hire",
                "price", 500.00,
                "currency", "INR",
                "bookingType", "RENTAL",
                "pricingUnit", "HOUR"
        ), status().isCreated());
        String serviceId = service.get("id").asText();
        assertThat(service.get("pricingUnit").asText()).isEqualTo("HOUR");

        JsonNode jcb1 = postJson("/api/v1/businesses/" + businessId + "/resources", ownerToken, Map.of(
                "name", "JCB-01",
                "type", "EXCAVATOR",
                "identifier", "MH-01-1234"
        ), status().isCreated());
        String jcb1Id = jcb1.get("id").asText();
        assertThat(jcb1.get("status").asText()).isEqualTo("AVAILABLE");

        JsonNode jcb2 = postJson("/api/v1/businesses/" + businessId + "/resources", ownerToken, Map.of(
                "name", "JCB-02", "type", "EXCAVATOR"
        ), status().isCreated());
        String jcb2Id = jcb2.get("id").asText();

        postJson("/api/v1/businesses/" + businessId + "/publish", ownerToken, null, status().isOk());

        // resources show up on the public profile so a customer can pick one
        mockMvc.perform(get("/api/v1/discovery/businesses/" + business.get("slug").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources.length()").value(2));

        String customerToken = registerAndGetAccessToken(uniqueEmail("renter"), "password123", "Renter", "CUSTOMER");

        // availability for the resource: whole-day hourly slots, all free
        mockMvc.perform(get("/api/v1/businesses/" + businessId + "/availability")
                        .header("Authorization", bearer(customerToken))
                        .param("serviceId", serviceId)
                        .param("resourceId", jcb1Id)
                        .param("date", LocalDate.ofInstant(RENTAL_START, ZoneOffset.UTC).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("RENTAL"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.slots.length()").value(24));

        // 09:00 -> 12:30 = 3.5h, charged as 4 hourly units = 2000
        Instant end = RENTAL_START.plus(210, ChronoUnit.MINUTES);
        JsonNode booking = postJson("/api/v1/businesses/" + businessId + "/bookings", customerToken, orderedMap(
                "serviceId", serviceId,
                "resourceId", jcb1Id,
                "startAt", RENTAL_START.toString(),
                "endAt", end.toString()
        ), status().isCreated());
        String bookingId = booking.get("id").asText();
        assertThat(booking.get("status").asText()).isEqualTo("PENDING");
        assertThat(booking.get("resourceId").asText()).isEqualTo(jcb1Id);
        assertThat(booking.get("price").decimalValue()).isEqualByComparingTo("2000.00");
        assertThat(booking.get("currency").asText()).isEqualTo("INR");

        // §34: an overlapping rental of the SAME resource is rejected
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/bookings")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderedMap(
                                "serviceId", serviceId,
                                "resourceId", jcb1Id,
                                "startAt", RENTAL_START.plus(1, ChronoUnit.HOURS).toString(),
                                "endAt", RENTAL_START.plus(5, ChronoUnit.HOURS).toString()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOKING_CONFLICT"));

        // the same window on a DIFFERENT resource is fine
        postJson("/api/v1/businesses/" + businessId + "/bookings", customerToken, orderedMap(
                "serviceId", serviceId,
                "resourceId", jcb2Id,
                "startAt", RENTAL_START.toString(),
                "endAt", end.toString()
        ), status().isCreated());

        // a back-to-back window on the original resource is also fine
        postJson("/api/v1/businesses/" + businessId + "/bookings", customerToken, orderedMap(
                "serviceId", serviceId,
                "resourceId", jcb1Id,
                "startAt", end.toString(),
                "endAt", end.plus(1, ChronoUnit.HOURS).toString()
        ), status().isCreated());

        // the booked hours are now unavailable in the availability view
        mockMvc.perform(get("/api/v1/businesses/" + businessId + "/availability")
                        .header("Authorization", bearer(customerToken))
                        .param("serviceId", serviceId)
                        .param("resourceId", jcb1Id)
                        .param("date", LocalDate.ofInstant(RENTAL_START, ZoneOffset.UTC).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots[9].available").value(false))
                .andExpect(jsonPath("$.slots[0].available").value(true));

        // owner walks the rental through its lifecycle
        for (Map.Entry<String, String> step : orderedSteps().entrySet()) {
            mockMvc.perform(post("/api/v1/businesses/" + businessId + "/bookings/" + bookingId + step.getKey())
                            .header("Authorization", bearer(ownerToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(step.getValue()));
        }

        // a soft-deleted (UNAVAILABLE) resource drops off the public profile
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/businesses/" + businessId + "/resources/" + jcb2Id)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/discovery/businesses/" + business.get("slug").asText()))
                .andExpect(jsonPath("$.resources.length()").value(1));
    }

    @Test
    void rentalFieldRulesAndResourceStatusAreEnforced() throws Exception {
        String ownerToken = registerAndGetAccessToken(uniqueEmail("rules-owner"), "password123", "Owner", "BUSINESS_OWNER");
        JsonNode business = postJson("/api/v1/businesses", ownerToken, Map.of(
                "name", "Rules Rentals",
                "category", "CAR_RENTAL",
                "capabilities", List.of("RENTALS", "RESOURCES", "APPOINTMENTS")
        ), status().isCreated());
        String businessId = business.get("id").asText();

        // a RENTAL service without a pricingUnit is rejected at creation
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/services")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Unpriced", "price", 10.0, "currency", "INR", "bookingType", "RENTAL"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        String rentalServiceId = postJson("/api/v1/businesses/" + businessId + "/services", ownerToken, Map.of(
                "name", "Car Hire", "price", 1200.00, "currency", "INR",
                "bookingType", "RENTAL", "pricingUnit", "DAY"
        ), status().isCreated()).get("id").asText();

        String appointmentServiceId = postJson("/api/v1/businesses/" + businessId + "/services", ownerToken, Map.of(
                "name", "Inspection", "price", 100.00, "currency", "INR",
                "durationMinutes", 30, "bookingType", "APPOINTMENT"
        ), status().isCreated()).get("id").asText();

        String carId = postJson("/api/v1/businesses/" + businessId + "/resources", ownerToken, Map.of(
                "name", "Swift-01"
        ), status().isCreated()).get("id").asText();

        postJson("/api/v1/businesses/" + businessId + "/publish", ownerToken, null, status().isOk());
        String customerToken = registerAndGetAccessToken(uniqueEmail("rules-cust"), "password123", "Cust", "CUSTOMER");

        // RENTAL without endAt
        expectValidationError(businessId, customerToken, orderedMap(
                "serviceId", rentalServiceId, "resourceId", carId, "startAt", RENTAL_START.toString()));

        // RENTAL with endAt not after startAt
        expectValidationError(businessId, customerToken, orderedMap(
                "serviceId", rentalServiceId, "resourceId", carId,
                "startAt", RENTAL_START.toString(), "endAt", RENTAL_START.toString()));

        // RENTAL without resourceId
        expectValidationError(businessId, customerToken, orderedMap(
                "serviceId", rentalServiceId, "startAt", RENTAL_START.toString(),
                "endAt", RENTAL_START.plus(1, ChronoUnit.DAYS).toString()));

        // APPOINTMENT must not carry a resourceId
        expectValidationError(businessId, customerToken, orderedMap(
                "serviceId", appointmentServiceId, "resourceId", carId, "startAt", RENTAL_START.toString()));

        // a resource in MAINTENANCE cannot be booked
        mockMvc.perform(patch("/api/v1/businesses/" + businessId + "/resources/" + carId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "MAINTENANCE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MAINTENANCE"));

        expectValidationError(businessId, customerToken, orderedMap(
                "serviceId", rentalServiceId, "resourceId", carId,
                "startAt", RENTAL_START.toString(),
                "endAt", RENTAL_START.plus(1, ChronoUnit.DAYS).toString()));

        // ... and its availability reads UNAVAILABLE with no slots
        mockMvc.perform(get("/api/v1/businesses/" + businessId + "/availability")
                        .header("Authorization", bearer(customerToken))
                        .param("serviceId", rentalServiceId)
                        .param("resourceId", carId)
                        .param("date", LocalDate.ofInstant(RENTAL_START, ZoneOffset.UTC).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.slots.length()").value(0));

        // back to AVAILABLE: a 2-day-and-a-bit hire is charged 3 daily units
        mockMvc.perform(patch("/api/v1/businesses/" + businessId + "/resources/" + carId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "AVAILABLE"))))
                .andExpect(status().isOk());

        JsonNode booking = postJson("/api/v1/businesses/" + businessId + "/bookings", customerToken, orderedMap(
                "serviceId", rentalServiceId,
                "resourceId", carId,
                "startAt", RENTAL_START.toString(),
                "endAt", RENTAL_START.plus(49, ChronoUnit.HOURS).toString()
        ), status().isCreated());
        assertThat(booking.get("price").decimalValue()).isEqualByComparingTo("3600.00");
    }

    private void expectValidationError(String businessId, String token, Map<String, ?> body) throws Exception {
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/bookings")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private static Map<String, String> orderedSteps() {
        Map<String, String> steps = new LinkedHashMap<>();
        steps.put("/confirm", "CONFIRMED");
        steps.put("/check-in", "CHECKED_IN");
        steps.put("/start", "IN_PROGRESS");
        steps.put("/complete", "COMPLETED");
        return steps;
    }

    private static Map<String, Object> orderedMap(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
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
