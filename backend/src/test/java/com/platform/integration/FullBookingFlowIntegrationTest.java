package com.platform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.platform.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end walk of the Phase 1-3 happy path named in CLAUDE_CODE.md §45:
 * register -> login -> create business -> add service/staff/hours -> publish
 * -> discovery finds it -> customer books -> owner confirms/completes.
 */
class FullBookingFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void ownerBuildsABusinessAndACustomerBooksAndCompletesAnAppointment() throws Exception {
        // 1. register the business owner
        String ownerEmail = uniqueEmail("owner");
        String ownerToken = registerAndGetAccessToken(ownerEmail, "password123", "Priya Owner", "BUSINESS_OWNER");

        // 2. create the business (starts DRAFT)
        JsonNode business = postJson("/api/v1/businesses", ownerToken, Map.of(
                "name", "Glow Salon",
                "description", "A lovely salon",
                "phone", "+911234567890",
                "email", "salon@example.com",
                "category", "SALON",
                "capabilities", java.util.List.of("APPOINTMENTS", "STAFF")
        ), status().isCreated());
        String businessId = business.get("id").asText();
        assertThat(business.get("status").asText()).isEqualTo("DRAFT");
        assertThat(business.get("slug").asText()).isEqualTo("glow-salon");

        // 3. add a bookable service
        JsonNode service = postJson("/api/v1/businesses/" + businessId + "/services", ownerToken, Map.of(
                "name", "Haircut",
                "description", "Classic haircut",
                "price", 200.00,
                "currency", "INR",
                "durationMinutes", 30,
                "bookingType", "APPOINTMENT"
        ), status().isCreated());
        String serviceId = service.get("id").asText();

        // 4. add a staff member assigned to that service
        JsonNode staff = postJson("/api/v1/businesses/" + businessId + "/staff", ownerToken, Map.of(
                "displayName", "Asha",
                "title", "Senior Stylist",
                "serviceIds", java.util.List.of(serviceId)
        ), status().isCreated());
        String staffId = staff.get("id").asText();

        // 5. set business hours for the day we're going to book on
        LocalDate bookingDate = LocalDate.now(ZoneOffset.UTC).plusDays(14);
        DayOfWeek dayOfWeek = bookingDate.getDayOfWeek();
        putJson("/api/v1/businesses/" + businessId + "/hours", ownerToken, Map.of(
                "hours", java.util.List.of(Map.of(
                        "dayOfWeek", dayOfWeek.name(),
                        "openTime", "09:00",
                        "closeTime", "18:00"
                ))
        ), status().isOk());

        // 6. publish -> ACTIVE
        MvcResult publishResult = mockMvc.perform(post("/api/v1/businesses/" + businessId + "/publish")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode published = objectMapper.readTree(publishResult.getResponse().getContentAsString());
        assertThat(published.get("status").asText()).isEqualTo("ACTIVE");

        // 7. discovery finds it publicly, with no auth
        mockMvc.perform(get("/api/v1/discovery/businesses/glow-salon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Glow Salon"))
                .andExpect(jsonPath("$.services[0].name").value("Haircut"))
                .andExpect(jsonPath("$.staff[0].displayName").value("Asha"));

        mockMvc.perform(get("/api/v1/discovery/businesses").param("query", "Glow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("glow-salon"));

        // 8. register a customer and check availability
        String customerEmail = uniqueEmail("customer");
        String customerToken = registerAndGetAccessToken(customerEmail, "password123", "Cara Customer", "CUSTOMER");

        MvcResult availabilityResult = mockMvc.perform(get("/api/v1/businesses/" + businessId + "/availability")
                        .header("Authorization", bearer(customerToken))
                        .param("serviceId", serviceId)
                        .param("date", bookingDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andReturn();
        JsonNode availability = objectMapper.readTree(availabilityResult.getResponse().getContentAsString());
        String slotStart = availability.get("slots").get(0).get("start").asText();

        // 9. customer books that slot
        JsonNode booking = postJson("/api/v1/businesses/" + businessId + "/bookings", customerToken, Map.of(
                "serviceId", serviceId,
                "staffId", staffId,
                "startAt", slotStart,
                "notes", "First time customer"
        ), status().isCreated());
        String bookingId = booking.get("id").asText();
        assertThat(booking.get("status").asText()).isEqualTo("PENDING");
        assertThat(booking.get("price").asDouble()).isEqualTo(200.00);
        assertThat(booking.get("currency").asText()).isEqualTo("INR");

        // 10. the same slot is no longer offered as available
        MvcResult afterBookingAvailability = mockMvc.perform(get("/api/v1/businesses/" + businessId + "/availability")
                        .header("Authorization", bearer(customerToken))
                        .param("serviceId", serviceId)
                        .param("staffId", staffId)
                        .param("date", bookingDate.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode afterSlots = objectMapper.readTree(afterBookingAvailability.getResponse().getContentAsString()).get("slots");
        boolean sameSlotStillAvailable = false;
        for (JsonNode slot : afterSlots) {
            if (slot.get("start").asText().equals(slotStart) && slot.get("available").asBoolean()) {
                sameSlotStillAvailable = true;
            }
        }
        assertThat(sameSlotStillAvailable).isFalse();

        // 11. a second, overlapping booking attempt for the same staff is rejected
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/bookings")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "serviceId", serviceId,
                                "staffId", staffId,
                                "startAt", slotStart
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOKING_CONFLICT"));

        // 12. owner cannot skip straight to COMPLETED (invalid transition)
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/bookings/" + bookingId + "/complete")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));

        // 13. owner walks the booking through its lifecycle
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/bookings/" + bookingId + "/confirm")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/bookings/" + bookingId + "/check-in")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"));
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/bookings/" + bookingId + "/start")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mockMvc.perform(post("/api/v1/businesses/" + businessId + "/bookings/" + bookingId + "/complete")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // 14. the customer can see it in their own booking history
        mockMvc.perform(get("/api/v1/me/bookings")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(bookingId))
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"));

        // 15. and a BusinessCustomer relationship + CustomerProfile now exist for them
        mockMvc.perform(get("/api/v1/users/me/customer-profile")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists());
    }

    private JsonNode postJson(String url, String token, Map<String, ?> body, org.springframework.test.web.servlet.ResultMatcher expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(expectedStatus)
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode putJson(String url, String token, Map<String, ?> body, org.springframework.test.web.servlet.ResultMatcher expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(put(url)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(expectedStatus)
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
