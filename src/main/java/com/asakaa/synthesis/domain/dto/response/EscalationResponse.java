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

    private Long id;
    private String escalationId;
    private Long consultationId;
    private String specialistType;
    private String urgencyLevel;
    private String status;
    private String caseSummary;
    private String referralNotes;
    private LocalDateTime submittedAt;
}
