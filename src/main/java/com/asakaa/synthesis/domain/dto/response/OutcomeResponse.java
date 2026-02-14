package com.asakaa.synthesis.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutcomeResponse {

    private String condition;
    private Long totalCases;
    private Long resolvedCases;
    private Long escalatedCases;
    private Double averageConfidenceScore;
}
