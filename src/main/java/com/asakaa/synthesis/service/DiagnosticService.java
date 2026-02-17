package com.asakaa.synthesis.service;

import com.asakaa.synthesis.domain.dto.request.DiagnosticRequest;
import com.asakaa.synthesis.domain.dto.response.DiagnosticResponse;
import com.asakaa.synthesis.domain.dto.response.DifferentialDto;
import com.asakaa.synthesis.domain.entity.Consultation;
import com.asakaa.synthesis.domain.entity.ConsultationStatus;
import com.asakaa.synthesis.domain.entity.Diagnosis;
import com.asakaa.synthesis.domain.entity.Patient;
import com.asakaa.synthesis.exception.DiagnosticException;
import com.asakaa.synthesis.exception.ResourceNotFoundException;
import com.asakaa.synthesis.integration.bedrock.BedrockClient;
import com.asakaa.synthesis.integration.bedrock.BedrockPromptBuilder;
import com.asakaa.synthesis.integration.bedrock.ClinicalContext;
import com.asakaa.synthesis.repository.ConsultationRepository;
import com.asakaa.synthesis.repository.DiagnosisRepository;
import com.asakaa.synthesis.util.ResponseParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosticService {

    private final ConsultationRepository consultationRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final BedrockPromptBuilder bedrockPromptBuilder;
    private final BedrockClient bedrockClient;
    private final ResponseParser responseParser;

    @Transactional
    public DiagnosticResponse analyze(DiagnosticRequest request) {
        log.info("Starting diagnostic analysis for consultation ID: {}", request.getConsultationId());

        // Fetch consultation
        Consultation consultation = consultationRepository.findById(request.getConsultationId())
                .orElseThrow(() -> new ResourceNotFoundException("Consultation", request.getConsultationId()));

        Patient patient = consultation.getPatient();

        // Build clinical context
        ClinicalContext context = buildClinicalContext(consultation, patient, request);

        // Generate prompt
        String prompt = bedrockPromptBuilder.buildDiagnosticPrompt(context);
        log.debug("Generated diagnostic prompt for consultation ID: {}", request.getConsultationId());

        // Invoke Bedrock
        String rawResponse;
        try {
            rawResponse = bedrockClient.invoke(prompt);
        } catch (Exception e) {
            log.error("Failed to invoke Bedrock for consultation ID: {}", request.getConsultationId(), e);
            throw new DiagnosticException(
                    "Failed to generate diagnostic analysis for consultation " + request.getConsultationId(), e);
        }

        // Parse response
        List<DifferentialDto> differentials;
        try {
            differentials = responseParser.parseDiagnosticResponse(rawResponse);
        } catch (Exception e) {
            log.error("Failed to parse Bedrock response for consultation ID: {}", request.getConsultationId(), e);
            throw new DiagnosticException(
                    "Failed to parse diagnostic response for consultation " + request.getConsultationId(), e);
        }

        log.info("Received {} differentials for consultation ID: {}", differentials.size(), request.getConsultationId());

        // Save high-confidence diagnoses
        for (DifferentialDto differential : differentials) {
            if (differential.getConfidence().compareTo(BigDecimal.valueOf(0.5)) > 0) {
                Diagnosis diagnosis = Diagnosis.builder()
                        .consultation(consultation)
                        .conditionName(differential.getCondition())
                        .confidenceScore(differential.getConfidence())
                        .reasoning(differential.getReasoning())
                        .source("AI_BEDROCK")
                        .build();
                diagnosisRepository.save(diagnosis);
                differential.setId(diagnosis.getId());
                log.debug("Saved diagnosis: {} with confidence: {}", 
                        differential.getCondition(), differential.getConfidence());
            }
        }

        // Update consultation status
        consultation.setStatus(ConsultationStatus.IN_PROGRESS);
        consultationRepository.save(consultation);

        // Build response
        DiagnosticResponse response = DiagnosticResponse.builder()
                .consultationId(consultation.getId())
                .differentials(differentials)
                .immediateActions(extractImmediateActions(rawResponse))
                .safetyNotes(extractSafetyNotes(rawResponse))
                .generatedAt(LocalDateTime.now())
                .build();

        log.info("Diagnostic analysis completed for consultation ID: {} with {} differentials", 
                request.getConsultationId(), differentials.size());

        return response;
    }

    private ClinicalContext buildClinicalContext(Consultation consultation, Patient patient, DiagnosticRequest request) {
        // Calculate patient age
        int age = Period.between(patient.getDateOfBirth(), LocalDateTime.now().toLocalDate()).getYears();

        // Build patient summary
        String patientSummary = String.format(
                "Age: %d years, Gender: %s, Blood Group: %s, Allergies: %s",
                age,
                patient.getGender() != null ? patient.getGender() : "Not specified",
                patient.getBloodGroup() != null ? patient.getBloodGroup() : "Not specified",
                patient.getAllergies() != null ? patient.getAllergies() : "None reported"
        );

        // Join equipment and formulary
        String equipment = request.getAvailableEquipment() != null && !request.getAvailableEquipment().isEmpty()
                ? String.join(", ", request.getAvailableEquipment())
                : "Standard primary care equipment";

        String formulary = request.getLocalFormulary() != null && !request.getLocalFormulary().isEmpty()
                ? String.join(", ", request.getLocalFormulary())
                : "WHO Essential Medicines List";

        // Build lab results from notes
        String labResults = consultation.getNotes() != null ? consultation.getNotes() : "No lab results available";
        if (request.getAdditionalNotes() != null) {
            labResults += "\nAdditional notes: " + request.getAdditionalNotes();
        }

        return ClinicalContext.builder()
                .patientSummary(patientSummary)
                .chiefComplaint(consultation.getChiefComplaint())
                .vitals(consultation.getVitals() != null ? consultation.getVitals() : "Not recorded")
                .availableEquipment(equipment)
                .localFormulary(formulary)
                .labResults(labResults)
                .build();
    }

    private List<String> extractImmediateActions(String rawResponse) {
        // Simple extraction - in production, parse from JSON
        List<String> actions = new ArrayList<>();
        try {
            if (rawResponse.contains("immediateActions")) {
                // This is a simplified extraction - ResponseParser could be extended
                actions.add("Review differential diagnosis");
                actions.add("Monitor vital signs");
            }
        } catch (Exception e) {
            log.warn("Failed to extract immediate actions", e);
        }
        return actions;
    }

    private String extractSafetyNotes(String rawResponse) {
        // Simple extraction - in production, parse from JSON
        try {
            if (rawResponse.contains("safetyNotes")) {
                return "Refer to specialist if condition worsens or does not improve with initial treatment";
            }
        } catch (Exception e) {
            log.warn("Failed to extract safety notes", e);
        }
        return "Monitor patient closely and escalate if necessary";
    }

    public com.asakaa.synthesis.domain.dto.response.ImageAnalysisResponse analyzeImage(
            byte[] imageBytes, String mediaType, String clinicalContext) {
        log.info("Starting image analysis with media type: {}", mediaType);

        // Validate file type
        if (!mediaType.equals("image/jpeg") && !mediaType.equals("image/png")) {
            throw new com.asakaa.synthesis.exception.ValidationException(
                    "Invalid file type. Only JPEG and PNG images are supported.");
        }

        // Validate image size (max 5MB)
        if (imageBytes.length > 5 * 1024 * 1024) {
            throw new com.asakaa.synthesis.exception.ValidationException(
                    "Image file is too large. Maximum size is 5MB.");
        }

        // Build clinical prompt for image analysis
        String prompt = buildImageAnalysisPrompt(clinicalContext);

        // Invoke Bedrock with vision
        String rawResponse;
        try {
            rawResponse = bedrockClient.invokeVision(imageBytes, mediaType, prompt);
        } catch (Exception e) {
            log.error("Failed to analyze image", e);
            throw new DiagnosticException("Failed to analyze medical image: " + e.getMessage(), e);
        }

        // Parse response
        com.asakaa.synthesis.domain.dto.response.ImageAnalysisResponse response = parseImageAnalysisResponse(rawResponse);
        response.setAnalyzedAt(LocalDateTime.now());

        log.info("Image analysis completed successfully");
        return response;
    }

    private String buildImageAnalysisPrompt(String clinicalContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert radiologist and clinical diagnostician. ");
        prompt.append("Analyze this medical image and provide a detailed clinical assessment.\n\n");

        if (clinicalContext != null && !clinicalContext.trim().isEmpty()) {
            prompt.append("CLINICAL CONTEXT:\n");
            prompt.append(clinicalContext).append("\n\n");
        }

        prompt.append("INSTRUCTIONS:\n");
        prompt.append("1. Describe what you see in the image in clinical terms\n");
        prompt.append("2. Identify any abnormalities, lesions, or pathological findings\n");
        prompt.append("3. Note the quality and technical adequacy of the image\n");
        prompt.append("4. Provide differential diagnoses based on the imaging findings\n");
        prompt.append("5. Suggest any additional imaging or tests that may be needed\n\n");

        prompt.append("Return your response as a JSON object with this structure:\n");
        prompt.append("{\n");
        prompt.append("  \"description\": \"Detailed description of the image and findings\",\n");
        prompt.append("  \"findings\": [\"finding1\", \"finding2\", \"finding3\"]\n");
        prompt.append("}\n\n");
        prompt.append("Return ONLY the JSON object, no additional text.");

        return prompt.toString();
    }

    private com.asakaa.synthesis.domain.dto.response.ImageAnalysisResponse parseImageAnalysisResponse(String rawResponse) {
        try {
            // Extract JSON from response if it contains markdown code blocks
            String jsonContent = extractJson(rawResponse);

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(jsonContent);

            String description = root.has("description") ? root.get("description").asText() : rawResponse;
            List<String> findings = new ArrayList<>();

            if (root.has("findings") && root.get("findings").isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode finding : root.get("findings")) {
                    findings.add(finding.asText());
                }
            }

            return com.asakaa.synthesis.domain.dto.response.ImageAnalysisResponse.builder()
                    .description(description)
                    .findings(findings)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to parse structured response, returning raw text", e);
            // Fallback: return raw response as description
            return com.asakaa.synthesis.domain.dto.response.ImageAnalysisResponse.builder()
                    .description(rawResponse)
                    .findings(new ArrayList<>())
                    .build();
        }
    }

    private String extractJson(String rawResponse) {
        String cleaned = rawResponse.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }
}
