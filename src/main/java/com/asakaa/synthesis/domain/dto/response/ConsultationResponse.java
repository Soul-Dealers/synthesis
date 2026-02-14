package com.asakaa.synthesis.domain.dto.response;

import com.asakaa.synthesis.domain.entity.ConsultationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationResponse {

    private Long id;
    private Long patientId;
    private String patientName;
    private Long providerId;
    private String providerName;
    private ConsultationStatus status;
    private String chiefComplaint;
    private String vitals;
    private String notes;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private List<DiagnosisResponse> diagnoses;
}
