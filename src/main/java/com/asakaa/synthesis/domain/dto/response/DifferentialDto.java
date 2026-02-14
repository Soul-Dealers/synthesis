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
public class DifferentialDto {

    private String condition;
    private BigDecimal confidence;
    private String reasoning;
    private List<String> recommendedTests;
    private List<String> redFlags;
}
