package com.asakaa.synthesis.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreatmentPlanResponse {

    private Long diagnosisId;
    private String conditionName;
    private List<TreatmentResponse> treatments;
    private String followUpInstructions;
    private String patientEducation;
    private LocalDateTime generatedAt;
}
