package com.asakaa.synthesis;

import com.asakaa.synthesis.domain.dto.request.ConsultationRequest;
import com.asakaa.synthesis.domain.dto.request.PatientRequest;
import com.asakaa.synthesis.domain.dto.response.ConsultationResponse;
import com.asakaa.synthesis.domain.dto.response.PatientResponse;
import com.asakaa.synthesis.domain.entity.ConsultationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ConsultationApiIntegrationTest extends BaseIntegrationTest {

    private String authToken;
    private Long testPatientId;

    @BeforeEach
    void setUp() {
        // Register and authenticate a test provider
        authToken = getAuthToken("consult" + System.currentTimeMillis() + "@example.com", "password123", "Dr. Consult");

        // Create a test patient
        PatientRequest patientRequest = PatientRequest.builder()
                .nationalId("CONSULT" + System.currentTimeMillis())
                .firstName("Test")
                .lastName("Patient")
                .dateOfBirth(LocalDate.of(1985, 3, 15))
                .gender("Female")
                .build();

        ResponseEntity<PatientResponse> patientResponse = restTemplate.exchange(
                "/api/v1/patients",
                HttpMethod.POST,
                createAuthEntity(patientRequest, authToken),
                PatientResponse.class
        );

        testPatientId = patientResponse.getBody().getId();
    }

    @Test
    void openConsultation_Returns201WithStatusOpen() {
        // Arrange
        ConsultationRequest request = ConsultationRequest.builder()
                .patientId(testPatientId)
                .chiefComplaint("Fever and headache")
                .vitals("{\"temperature\": 38.5, \"bp\": \"120/80\"}")
                .notes("Patient reports symptoms for 2 days")
                .build();

        HttpEntity<ConsultationRequest> entity = createAuthEntity(request, authToken);

        // Act
        ResponseEntity<ConsultationResponse> response = restTemplate.exchange(
                "/api/v1/consultations",
                HttpMethod.POST,
                entity,
                ConsultationResponse.class
        );

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ConsultationStatus.OPEN, response.getBody().getStatus());
        assertEquals("Fever and headache", response.getBody().getChiefComplaint());
        assertNotNull(response.getBody().getOpenedAt());
        assertNull(response.getBody().getClosedAt());
    }

    @Test
    void closeConsultation_ChangesStatusToClosed() {
        // Arrange - Create a consultation first
        ConsultationRequest request = ConsultationRequest.builder()
                .patientId(testPatientId)
                .chiefComplaint("Cough")
                .build();

        ResponseEntity<ConsultationResponse> createResponse = restTemplate.exchange(
                "/api/v1/consultations",
                HttpMethod.POST,
                createAuthEntity(request, authToken),
                ConsultationResponse.class
        );

        Long consultationId = createResponse.getBody().getId();

        // Act
        ResponseEntity<ConsultationResponse> response = restTemplate.exchange(
                "/api/v1/consultations/" + consultationId + "/close",
                HttpMethod.POST,
                new HttpEntity<>(createAuthHeaders(authToken)),
                ConsultationResponse.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ConsultationStatus.CLOSED, response.getBody().getStatus());
        assertNotNull(response.getBody().getClosedAt());
    }

    @Test
    void getConsultationById_ReturnsFullConsultationWithPatientName() {
        // Arrange - Create a consultation first
        ConsultationRequest request = ConsultationRequest.builder()
                .patientId(testPatientId)
                .chiefComplaint("Back pain")
                .vitals("{\"bp\": \"130/85\"}")
                .build();

        ResponseEntity<ConsultationResponse> createResponse = restTemplate.exchange(
                "/api/v1/consultations",
                HttpMethod.POST,
                createAuthEntity(request, authToken),
                ConsultationResponse.class
        );

        Long consultationId = createResponse.getBody().getId();

        // Act
        ResponseEntity<ConsultationResponse> response = restTemplate.exchange(
                "/api/v1/consultations/" + consultationId,
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders(authToken)),
                ConsultationResponse.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(consultationId, response.getBody().getId());
        assertEquals("Test Patient", response.getBody().getPatientName());
        assertEquals("Back pain", response.getBody().getChiefComplaint());
        assertNotNull(response.getBody().getProviderName());
    }
}
