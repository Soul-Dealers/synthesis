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
public class DiagnosticRequest {

    @NotNull(message = "Consultation ID is required")
    private Long consultationId;

    private List<String> availableEquipment;
    private List<String> localFormulary;
    private String additionalNotes;
}
