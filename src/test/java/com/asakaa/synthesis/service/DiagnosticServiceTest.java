package com.asakaa.synthesis.service;

import com.asakaa.synthesis.domain.dto.request.DiagnosticRequest;
import com.asakaa.synthesis.domain.dto.response.DiagnosticResponse;
import com.asakaa.synthesis.domain.dto.response.DifferentialDto;
import com.asakaa.synthesis.domain.entity.Consultation;
import com.asakaa.synthesis.domain.entity.ConsultationStatus;
import com.asakaa.synthesis.domain.entity.Patient;
import com.asakaa.synthesis.domain.entity.Provider;
import com.asakaa.synthesis.exception.DiagnosticException;
import com.asakaa.synthesis.exception.ResourceNotFoundException;
import com.asakaa.synthesis.integration.bedrock.BedrockClient;
import com.asakaa.synthesis.integration.bedrock.BedrockPromptBuilder;
import com.asakaa.synthesis.integration.bedrock.ClinicalContext;
import com.asakaa.synthesis.repository.ConsultationRepository;
import com.asakaa.synthesis.repository.DiagnosisRepository;
import com.asakaa.synthesis.util.ResponseParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiagnosticServiceTest {

    @Mock
    private ConsultationRepository consultationRepository;

    @Mock
    private DiagnosisRepository diagnosisRepository;

    @Mock
    private BedrockPromptBuilder bedrockPromptBuilder;

    @Mock
    private BedrockClient bedrockClient;

    @Mock
    private ResponseParser responseParser;

    @InjectMocks
    private DiagnosticService diagnosticService;

    private DiagnosticRequest request;
    private Consultation consultation;
    private Patient patient;
    private Provider provider;

    @BeforeEach
    void setUp() {
        request = DiagnosticRequest.builder()
                .consultationId(1L)
                .availableEquipment(List.of("Stethoscope", "Thermometer"))
                .localFormulary(List.of("Paracetamol", "Amoxicillin"))
                .build();

        patient = Patient.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender("Male")
                .bloodGroup("O+")
                .build();

        provider = Provider.builder()
                .id(1L)
                .name("Dr. Smith")
                .build();

        consultation = Consultation.builder()
                .id(1L)
                .patient(patient)
                .provider(provider)
                .status(ConsultationStatus.OPEN)
                .chiefComplaint("Fever and cough")
                .vitals("{\"temperature\": 38.5}")
                .openedAt(LocalDateTime.now())
                .diagnoses(new ArrayList<>())
                .build();
    }

    @Test
    void analyze_ReturnsCorrectNumberOfDifferentials() {
        // Arrange
        List<DifferentialDto> differentials = List.of(
                DifferentialDto.builder()
                        .condition("Malaria")
                        .confidence(BigDecimal.valueOf(0.85))
                        .reasoning("High fever in endemic area")
                        .build(),
                DifferentialDto.builder()
                        .condition("Pneumonia")
                        .confidence(BigDecimal.valueOf(0.65))
                        .reasoning("Cough and fever")
                        .build()
        );

        when(consultationRepository.findById(1L)).thenReturn(Optional.of(consultation));
        when(bedrockPromptBuilder.buildDiagnosticPrompt(any(ClinicalContext.class))).thenReturn("prompt");
        when(bedrockClient.invoke(anyString())).thenReturn("{\"differentials\": []}");
        when(responseParser.parseDiagnosticResponse(anyString())).thenReturn(differentials);

        // Act
        DiagnosticResponse response = diagnosticService.analyze(request);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getDifferentials().size());
        assertEquals(1L, response.getConsultationId());
        verify(consultationRepository).save(any(Consultation.class));
    }

    @Test
    void analyze_ThrowsResourceNotFoundException_WhenConsultationNotFound() {
        // Arrange
        when(consultationRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> diagnosticService.analyze(request));
        verify(bedrockClient, never()).invoke(anyString());
    }

    @Test
    void analyze_ThrowsDiagnosticException_WhenBedrockClientThrows() {
        // Arrange
        when(consultationRepository.findById(1L)).thenReturn(Optional.of(consultation));
        when(bedrockPromptBuilder.buildDiagnosticPrompt(any(ClinicalContext.class))).thenReturn("prompt");
        when(bedrockClient.invoke(anyString())).thenThrow(new DiagnosticException("Bedrock error"));

        // Act & Assert
        assertThrows(DiagnosticException.class, () -> diagnosticService.analyze(request));
    }
}
