package com.asakaa.synthesis.domain.dto.response;

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
public class ClinicResponse {

    private Long id;
    private String name;
    private String address;
    private String region;
    private String registrationCode;
    private LocalDateTime createdAt;
    private List<ProviderSummary> staff;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderSummary {
        private Long id;
        private String name;
        private String role;
        private String email;
    }
}
