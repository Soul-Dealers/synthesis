package com.asakaa.synthesis.domain.dto.response;

import com.asakaa.synthesis.domain.entity.ReferralStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralResponse {

    private Long id;
    private Long patientId;
    private String patientName;
    private Long referringClinicId;
    private String referringClinicName;
    private Long receivingClinicId;
    private String receivingClinicName;
    private Long referringProviderId;
    private String referringProviderName;
    private String reason;
    private String notes;
    private LocalDateTime accessExpiresAt;
    private ReferralStatus status;
    private LocalDateTime acceptedAt;
    private String acceptedByProviderName;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private LocalDateTime createdAt;
}
