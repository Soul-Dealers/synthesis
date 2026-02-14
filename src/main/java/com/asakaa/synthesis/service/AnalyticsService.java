package com.asakaa.synthesis.service;

import com.asakaa.synthesis.domain.dto.response.OutcomeResponse;
import com.asakaa.synthesis.domain.dto.response.TrendResponse;
import com.asakaa.synthesis.domain.entity.ConsultationStatus;
import com.asakaa.synthesis.repository.ConsultationRepository;
import com.asakaa.synthesis.repository.DiagnosisRepository;
import com.asakaa.synthesis.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ConsultationRepository consultationRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final PatientRepository patientRepository;

    public List<TrendResponse> getDiseaseTrends(String region, LocalDate from, LocalDate to) {
        log.info("Fetching disease trends for region: {}, from: {}, to: {}", region, from, to);

        LocalDateTime startDate = from != null ? from.atStartOfDay() : LocalDateTime.now().minusMonths(3);
        LocalDateTime endDate = to != null ? to.atTime(23, 59, 59) : LocalDateTime.now();

        List<Map<String, Object>> results = consultationRepository.countByConditionAndRegion(startDate, endDate);

        return results.stream()
                .filter(result -> region == null || region.isEmpty() || 
                        (result.get("region") != null && result.get("region").toString().equalsIgnoreCase(region)))
                .map(result -> TrendResponse.builder()
                        .condition(result.get("condition") != null ? result.get("condition").toString() : "Unknown")
                        .caseCount(result.get("count") != null ? ((Number) result.get("count")).longValue() : 0L)
                        .region(result.get("region") != null ? result.get("region").toString() : "Unknown")
                        .period(String.format("%s to %s", startDate.toLocalDate(), endDate.toLocalDate()))
                        .build())
                .collect(Collectors.toList());
    }

    public List<OutcomeResponse> getTreatmentOutcomes() {
        log.info("Fetching treatment outcomes");

        // Get all diagnoses grouped by condition
        List<Object[]> diagnosisStats = diagnosisRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        diagnosis -> diagnosis.getConditionName(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                diagnoses -> new Object[]{
                                        diagnoses.size(),
                                        diagnoses.stream()
                                                .filter(d -> d.getConsultation().getStatus() == ConsultationStatus.CLOSED)
                                                .count(),
                                        0L, // Escalated cases - would need escalation tracking table
                                        diagnoses.stream()
                                                .filter(d -> d.getConfidenceScore() != null)
                                                .mapToDouble(d -> d.getConfidenceScore().doubleValue())
                                                .average()
                                                .orElse(0.0)
                                }
                        )
                ))
                .entrySet().stream()
                .map(entry -> new Object[]{entry.getKey(), entry.getValue()})
                .toList();

        return diagnosisStats.stream()
                .map(stat -> {
                    String condition = (String) stat[0];
                    Object[] values = (Object[]) stat[1];
                    return OutcomeResponse.builder()
                            .condition(condition)
                            .totalCases((Long) values[0])
                            .resolvedCases((Long) values[1])
                            .escalatedCases((Long) values[2])
                            .averageConfidenceScore((Double) values[3])
                            .build();
                })
                .sorted(Comparator.comparing(OutcomeResponse::getTotalCases).reversed())
                .collect(Collectors.toList());
    }

    public Map<String, Object> getClinicSummary(String clinicName) {
        log.info("Fetching clinic summary for: {}", clinicName);

        Map<String, Object> summary = new HashMap<>();

        // Total patients
        long totalPatients = patientRepository.findByClinicName(clinicName).size();
        summary.put("totalPatients", totalPatients);

        // Total consultations
        List<Long> patientIds = patientRepository.findByClinicName(clinicName).stream()
                .map(patient -> patient.getId())
                .collect(Collectors.toList());

        long totalConsultations = 0;
        long activeConsultations = 0;
        Map<String, Long> conditionCounts = new HashMap<>();

        for (Long patientId : patientIds) {
            List<com.asakaa.synthesis.domain.entity.Consultation> consultations = 
                    consultationRepository.findByPatientId(patientId);
            totalConsultations += consultations.size();

            activeConsultations += consultations.stream()
                    .filter(c -> c.getStatus() == ConsultationStatus.OPEN || 
                                 c.getStatus() == ConsultationStatus.IN_PROGRESS)
                    .count();

            // Count diagnoses
            consultations.forEach(consultation -> 
                consultation.getDiagnoses().forEach(diagnosis -> 
                    conditionCounts.merge(diagnosis.getConditionName(), 1L, Long::sum)
                )
            );
        }

        summary.put("totalConsultations", totalConsultations);
        summary.put("activeConsultations", activeConsultations);

        // Top condition
        String topCondition = conditionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");
        summary.put("topCondition", topCondition);

        return summary;
    }
}
