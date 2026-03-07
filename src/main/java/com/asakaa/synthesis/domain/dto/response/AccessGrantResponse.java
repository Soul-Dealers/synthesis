package com.asakaa.synthesis.domain.dto.response;

import com.asakaa.synthesis.domain.entity.PermissionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessGrantResponse {

    private Long id;
    private Long patientId;
    private String patientName;
    private Long clinicId;
    private String clinicName;
    private Long referralId;
    private PermissionType permissionType;
    private LocalDateTime expiresAt;
    private Boolean revoked;
    private LocalDateTime revokedAt;
    private String revokedBy;
    private String revocationReason;
    private LocalDateTime createdAt;
}
