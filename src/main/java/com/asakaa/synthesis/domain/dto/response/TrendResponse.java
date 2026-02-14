package com.asakaa.synthesis.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendResponse {

    private String condition;
    private Long caseCount;
    private String region;
    private String period;
}
