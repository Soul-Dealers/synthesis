package com.asakaa.synthesis;

import com.asakaa.synthesis.domain.dto.request.PatientRequest;
import com.asakaa.synthesis.domain.dto.response.PatientResponse;
import com.asakaa.synthesis.exception.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PatientApiIntegrationTest extends BaseIntegrationTest {

    private String authToken;

    @BeforeEach
    void setUp() {
        // Register and authenticate a test provider
        authToken = getAuthToken("provider" + System.currentTimeMillis() + "@example.com", "password123", "Dr. Provider");
    }

    @Test
    void createPatient_WithValidBody_Returns201() {
        // Arrange
        PatientRequest request = PatientRequest.builder()
                .nationalId("TEST" + System.currentTimeMillis())
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender("Male")
                .bloodGroup("O+")
                .clinicId(null)
                .build();

        HttpEntity<PatientRequest> entity = createAuthEntity(request, authToken);

        // Act
        ResponseEntity<PatientResponse> response = restTemplate.exchange(
                "/api/v1/patients",
                HttpMethod.POST,
                entity,
                PatientResponse.class
        );

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("John", response.getBody().getFirstName());
        assertEquals("Doe", response.getBody().getLastName());
        assertNotNull(response.getBody().getId());
    }

    @Test
    void createPatient_WithDuplicateNationalId_Returns400() {
        // Arrange
        String nationalId = "DUPLICATE" + System.currentTimeMillis();
        
        PatientRequest request1 = PatientRequest.builder()
                .nationalId(nationalId)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        // Create first patient
        restTemplate.exchange(
                "/api/v1/patients",
                HttpMethod.POST,
                createAuthEntity(request1, authToken),
                PatientResponse.class
        );

        // Try to create duplicate
        PatientRequest request2 = PatientRequest.builder()
                .nationalId(nationalId)
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.of(1995, 5, 5))
                .build();

        HttpEntity<PatientRequest> entity = createAuthEntity(request2, authToken);

        // Act
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/v1/patients",
                HttpMethod.POST,
                entity,
                ErrorResponse.class
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("already exists"));
    }

    @Test
    void getPatientById_ForExistingPatient_Returns200() {
        // Arrange - Create a patient first
        PatientRequest request = PatientRequest.builder()
                .nationalId("GET" + System.currentTimeMillis())
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.of(1995, 5, 5))
                .build();

        ResponseEntity<PatientResponse> createResponse = restTemplate.exchange(
                "/api/v1/patients",
                HttpMethod.POST,
                createAuthEntity(request, authToken),
                PatientResponse.class
        );

        Long patientId = createResponse.getBody().getId();

        // Act
        ResponseEntity<PatientResponse> response = restTemplate.exchange(
                "/api/v1/patients/" + patientId,
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(authToken)),
                PatientResponse.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Jane", response.getBody().getFirstName());
        assertEquals(patientId, response.getBody().getId());
    }

    @Test
    void getPatientById_ForUnknownId_Returns404() {
        // Act
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/v1/patients/99999",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(authToken)),
                ErrorResponse.class
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("not found"));
    }
}
