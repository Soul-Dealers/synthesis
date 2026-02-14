package com.asakaa.synthesis.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscalationResponse {

    private String escalationId;
    private Long consultationId;
    private String status;
    private String caseSummary;
    private LocalDateTime submittedAt;
    private String urgencyLevel;
}
