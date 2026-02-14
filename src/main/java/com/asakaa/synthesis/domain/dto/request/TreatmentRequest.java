package com.asakaa.synthesis.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreatmentRequest {

    @NotNull(message = "Diagnosis ID is required")
    private Long diagnosisId;

    private Double patientWeightKg;
    private Integer patientAgeYears;
    private Boolean renalFunctionNormal;
    private List<String> availableMedications;
}
