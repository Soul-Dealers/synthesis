package com.asakaa.synthesis.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscalationRequest {

    @NotNull(message = "Consultation ID is required")
    private Long consultationId;

    private String urgencyLevel; // ROUTINE, URGENT, EMERGENCY

    @NotBlank(message = "Specialist type is required")
    private String specialistType;

    private String referralNotes;
}
