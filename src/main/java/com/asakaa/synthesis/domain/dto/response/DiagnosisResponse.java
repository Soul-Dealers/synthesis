package com.asakaa.synthesis.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisResponse {

    private Long id;
    private String conditionName;
    private BigDecimal confidenceScore;
    private String reasoning;
    private String source;
    private List<TreatmentResponse> treatments;
}
