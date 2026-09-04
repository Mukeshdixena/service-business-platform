package com.platform.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared base for every integration test: spins up a real PostgreSQL container
 * (Flyway migrates it exactly like production would), boots the full Spring
 * context with MockMvc, and provides small helpers for the auth handshake so
 * individual tests can focus on the flow being verified.
 *
 * <p>The container is started exactly once per JVM via a static initializer
 * (the Testcontainers "singleton container" pattern) rather than a JUnit
 * {@code @Container}-managed field. With multiple test classes extending this
 * base, a per-class-managed container would be stopped and restarted (on a new
 * port) between classes while Spring's test context cache kept reusing the
 * first class's application context — leaving its DataSource pointed at a
 * now-dead port. Starting the container once and never stopping it (Ryuk reaps
 * it on JVM exit) keeps the JDBC URL stable for every test class that runs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("service_platform_test")
                .withUsername("service_platform")
                .withPassword("service_platform");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String uniqueEmail(String prefix) {
        return prefix + "-" + System.nanoTime() + "@example.com";
    }

    /** Registers a new user and returns the accessToken from the AuthResponse. */
    protected String registerAndGetAccessToken(String email, String password, String fullName, String role) throws Exception {
        String body = objectMapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
            put("email", email);
            put("password", password);
            put("fullName", fullName);
            put("role", role);
        }});
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    protected String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}
