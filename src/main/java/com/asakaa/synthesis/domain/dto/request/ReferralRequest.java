package com.asakaa.synthesis.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Receiving clinic ID is required")
    private Long receivingClinicId;

    @NotBlank(message = "Reason for referral is required")
    private String reason;

    private String notes;

    @NotNull(message = "Access duration in days is required")
    @Positive(message = "Access duration must be positive")
    private Integer accessDurationDays; // 7-30 days typically
}
