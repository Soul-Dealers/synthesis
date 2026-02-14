package com.asakaa.synthesis;

import com.asakaa.synthesis.domain.dto.request.RegisterRequest;
import com.asakaa.synthesis.domain.dto.response.AuthResponse;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("synthesis_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    protected TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    /**
     * Helper method to register a test provider and get authentication token
     */
    protected String getAuthToken() {
        return getAuthToken("test@example.com", "password123", "Dr. Test");
    }

    /**
     * Helper method to register a test provider with custom credentials and get authentication token
     */
    protected String getAuthToken(String email, String password, String name) {
        RegisterRequest request = RegisterRequest.builder()
                .name(name)
                .role("Doctor")
                .clinicName("Test Clinic")
                .region("Test Region")
                .email(email)
                .password(password)
                .build();

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register",
                request,
                AuthResponse.class
        );

        if (response.getBody() != null && response.getBody().getToken() != null) {
            return "Bearer " + response.getBody().getToken();
        }

        throw new RuntimeException("Failed to get auth token");
    }

    /**
     * Helper method to create HTTP headers with authentication
     */
    protected HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        return headers;
    }

    /**
     * Helper method to create HTTP entity with authentication
     */
    protected <T> HttpEntity<T> createAuthEntity(T body, String token) {
        return new HttpEntity<>(body, createAuthHeaders(token));
    }
}
