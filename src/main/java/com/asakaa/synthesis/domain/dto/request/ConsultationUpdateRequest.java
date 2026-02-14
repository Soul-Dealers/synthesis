package com.asakaa.synthesis.domain.dto.request;

import com.asakaa.synthesis.domain.entity.ConsultationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationUpdateRequest {

    private String vitals;
    private String notes;
    private ConsultationStatus status;
}
