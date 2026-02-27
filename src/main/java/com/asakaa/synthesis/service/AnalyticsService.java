package com.asakaa.synthesis.service;

import com.asakaa.synthesis.domain.dto.response.OutcomeResponse;
import com.asakaa.synthesis.domain.dto.response.TrendResponse;
import com.asakaa.synthesis.domain.entity.BaseEntity;
import com.asakaa.synthesis.domain.entity.ConsultationStatus;
import com.asakaa.synthesis.domain.entity.Diagnosis;
import com.asakaa.synthesis.repository.ConsultationRepository;
import com.asakaa.synthesis.repository.DiagnosisRepository;
import com.asakaa.synthesis.repository.EscalationRepository;
import com.asakaa.synthesis.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ConsultationRepository consultationRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final EscalationRepository escalationRepository;
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

        List<Diagnosis> allDiagnoses = diagnosisRepository.findAll();

        Map<String, List<Diagnosis>> grouped = allDiagnoses.stream()
                .collect(Collectors.groupingBy(Diagnosis::getConditionName));

        return grouped.entrySet().stream()
                .map(entry -> {
                    String condition = entry.getKey();
                    List<Diagnosis> diagnoses = entry.getValue();

                    long totalCases = diagnoses.size();

                    long resolvedCases = diagnoses.stream()
                            .filter(d -> d.getConsultation().getStatus() == ConsultationStatus.CLOSED)
                            .count();

                    long escalatedCases = diagnoses.stream()
                            .filter(d -> d.getConsultation().getStatus() == ConsultationStatus.ESCALATED)
                            .count();

                    double avgConfidence = diagnoses.stream()
                            .filter(d -> d.getConfidenceScore() != null)
                            .mapToDouble(d -> d.getConfidenceScore().doubleValue())
                            .average()
                            .orElse(0.0);

                    return OutcomeResponse.builder()
                            .condition(condition)
                            .totalCases(totalCases)
                            .resolvedCases(resolvedCases)
                            .escalatedCases(escalatedCases)
                            .averageConfidenceScore(avgConfidence)
                            .build();
                })
                .sorted(Comparator.comparing(OutcomeResponse::getTotalCases).reversed())
                .collect(Collectors.toList());
    }

    public Map<String, Object> getClinicSummary(Long clinicId) {
        log.info("Fetching clinic summary for clinic id: {}", clinicId);

        Map<String, Object> summary = new HashMap<>();

        List<Long> patientIds = patientRepository.findByClinicId(clinicId).stream()
                .map(BaseEntity::getId)
                .toList();

        summary.put("totalPatients", patientIds.size());

        long totalConsultations = 0;
        long activeConsultations = 0;
        long escalatedConsultations = 0;
        Map<String, Long> conditionCounts = new HashMap<>();

        for (Long patientId : patientIds) {
            var consultations = consultationRepository.findByPatientId(patientId);
            totalConsultations += consultations.size();

            activeConsultations += consultations.stream()
                    .filter(c -> c.getStatus() == ConsultationStatus.OPEN ||
                            c.getStatus() == ConsultationStatus.IN_PROGRESS)
                    .count();

            escalatedConsultations += consultations.stream()
                    .filter(c -> c.getStatus() == ConsultationStatus.ESCALATED)
                    .count();

            consultations.forEach(consultation ->
                    consultation.getDiagnoses().forEach(diagnosis ->
                            conditionCounts.merge(diagnosis.getConditionName(), 1L, Long::sum)
                    )
            );
        }

        summary.put("totalConsultations", totalConsultations);
        summary.put("activeConsultations", activeConsultations);
        summary.put("escalatedConsultations", escalatedConsultations);

        String topCondition = conditionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");
        summary.put("topCondition", topCondition);
        summary.put("conditionBreakdown", conditionCounts);

        return summary;
    }

    public Map<String, Object> getDashboardSummary() {
        log.info("Fetching dashboard summary");

        Map<String, Object> dashboard = new HashMap<>();

        dashboard.put("totalPatients", patientRepository.count());
        dashboard.put("totalConsultations", consultationRepository.count());
        dashboard.put("totalEscalations", escalationRepository.count());

        Map<String, Long> statusCounts = new HashMap<>();
        for (ConsultationStatus status : ConsultationStatus.values()) {
            long count = consultationRepository.findAll().stream()
                    .filter(c -> c.getStatus() == status)
                    .count();
            statusCounts.put(status.name(), count);
        }
        dashboard.put("consultationsByStatus", statusCounts);

        List<Diagnosis> allDiagnoses = diagnosisRepository.findAll();
        Map<String, Long> conditionCounts = allDiagnoses.stream()
                .collect(Collectors.groupingBy(Diagnosis::getConditionName, Collectors.counting()));

        List<Map<String, Object>> topConditions = conditionCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("condition", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
        dashboard.put("topConditions", topConditions);

        return dashboard;
    }
}
