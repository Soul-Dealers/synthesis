package com.asakaa.synthesis.service;

import com.asakaa.synthesis.domain.dto.request.EscalationRequest;
import com.asakaa.synthesis.domain.dto.response.EscalationResponse;
import com.asakaa.synthesis.domain.entity.Consultation;
import com.asakaa.synthesis.domain.entity.Diagnosis;
import com.asakaa.synthesis.domain.entity.Patient;
import com.asakaa.synthesis.domain.entity.Treatment;
import com.asakaa.synthesis.exception.ResourceNotFoundException;
import com.asakaa.synthesis.integration.telemedicine.TelemedicineAdapter;
import com.asakaa.synthesis.repository.ConsultationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.Comparator;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EscalationService {

    private final ConsultationRepository consultationRepository;
    private final TelemedicineAdapter telemedicineAdapter;
    private final NotificationService notificationService;

    @Transactional
    public EscalationResponse escalate(EscalationRequest request) {
        log.info("Processing escalation for consultation ID: {}", request.getConsultationId());

        // Fetch consultation with all related data
        Consultation consultation = consultationRepository.findById(request.getConsultationId())
                .orElseThrow(() -> new ResourceNotFoundException("Consultation", request.getConsultationId()));

        // Build comprehensive case summary
        String caseSummary = buildCaseSummary(consultation, request);

        // Send to telemedicine system
        String referralId = telemedicineAdapter.sendEscalation(
                caseSummary,
                request.getUrgencyLevel() != null ? request.getUrgencyLevel() : "ROUTINE",
                request.getSpecialistType()
        );

        // Send notifications
        notificationService.notifyEscalation(consultation.getId(), referralId);

        log.info("Escalation completed successfully. Referral ID: {}", referralId);

        // Build response
        return EscalationResponse.builder()
                .escalationId(referralId)
                .consultationId(consultation.getId())
                .status("SUBMITTED")
                .caseSummary(caseSummary)
                .submittedAt(LocalDateTime.now())
                .urgencyLevel(request.getUrgencyLevel() != null ? request.getUrgencyLevel() : "ROUTINE")
                .build();
    }

    private String buildCaseSummary(Consultation consultation, EscalationRequest request) {
        Patient patient = consultation.getPatient();
        
        // Calculate patient age
        int age = Period.between(patient.getDateOfBirth(), LocalDateTime.now().toLocalDate()).getYears();

        StringBuilder summary = new StringBuilder();
        
        // Header
        summary.append("=== SPECIALIST REFERRAL CASE SUMMARY ===\n\n");
        
        // Patient Demographics
        summary.append("PATIENT INFORMATION:\n");
        summary.append(String.format("- Name: %s %s\n", patient.getFirstName(), patient.getLastName()));
        summary.append(String.format("- Age: %d years\n", age));
        summary.append(String.format("- Gender: %s\n", patient.getGender() != null ? patient.getGender() : "Not specified"));
        summary.append(String.format("- Blood Group: %s\n", patient.getBloodGroup() != null ? patient.getBloodGroup() : "Not specified"));
        summary.append(String.format("- Allergies: %s\n", patient.getAllergies() != null ? patient.getAllergies() : "None reported"));
        summary.append(String.format("- Clinic: %s\n\n", patient.getClinicName() != null ? patient.getClinicName() : "Not specified"));

        // Consultation Details
        summary.append("CONSULTATION DETAILS:\n");
        summary.append(String.format("- Chief Complaint: %s\n", consultation.getChiefComplaint()));
        summary.append(String.format("- Vital Signs: %s\n", consultation.getVitals() != null ? consultation.getVitals() : "Not recorded"));
        summary.append(String.format("- Consultation Date: %s\n", consultation.getOpenedAt()));
        summary.append(String.format("- Provider: %s\n\n", consultation.getProvider().getName()));

        // Clinical Notes
        if (consultation.getNotes() != null && !consultation.getNotes().isEmpty()) {
            summary.append("CLINICAL NOTES:\n");
            summary.append(consultation.getNotes()).append("\n\n");
        }

        // Diagnoses
        if (!consultation.getDiagnoses().isEmpty()) {
            summary.append("DIFFERENTIAL DIAGNOSES:\n");
            consultation.getDiagnoses().stream()
                    .sorted(Comparator.comparing(Diagnosis::getConfidenceScore).reversed())
                    .forEach(diagnosis -> {
                        summary.append(String.format("- %s (Confidence: %.0f%%)\n",
                                diagnosis.getConditionName(),
                                diagnosis.getConfidenceScore().doubleValue() * 100));
                        summary.append(String.format("  Reasoning: %s\n", diagnosis.getReasoning()));
                    });
            summary.append("\n");
        }

        // Treatments
        if (!consultation.getDiagnoses().isEmpty()) {
            boolean hasTreatments = consultation.getDiagnoses().stream()
                    .anyMatch(d -> !d.getTreatments().isEmpty());
            
            if (hasTreatments) {
                summary.append("CURRENT TREATMENT PLAN:\n");
                consultation.getDiagnoses().forEach(diagnosis -> {
                    if (!diagnosis.getTreatments().isEmpty()) {
                        summary.append(String.format("For %s:\n", diagnosis.getConditionName()));
                        diagnosis.getTreatments().forEach(treatment -> {
                            summary.append(String.format("  - %s: %s, %s for %s\n",
                                    treatment.getType() != null ? treatment.getType() : "Treatment",
                                    treatment.getDrugName() != null ? treatment.getDrugName() : "Not specified",
                                    treatment.getDosage() != null ? treatment.getDosage() : "Dosage not specified",
                                    treatment.getDuration() != null ? treatment.getDuration() : "Duration not specified"));
                        });
                    }
                });
                summary.append("\n");
            }
        }

        // Referral Details
        summary.append("REFERRAL INFORMATION:\n");
        summary.append(String.format("- Specialist Type: %s\n", request.getSpecialistType()));
        summary.append(String.format("- Urgency Level: %s\n", request.getUrgencyLevel() != null ? request.getUrgencyLevel() : "ROUTINE"));
        if (request.getReferralNotes() != null && !request.getReferralNotes().isEmpty()) {
            summary.append(String.format("- Referral Notes: %s\n", request.getReferralNotes()));
        }

        return summary.toString();
    }
}
