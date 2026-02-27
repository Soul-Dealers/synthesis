package com.asakaa.synthesis.domain.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    private String name;
    private String role;
    private String clinicRegistrationCode;
    private String region;
    private String email;
    private String password;
}

