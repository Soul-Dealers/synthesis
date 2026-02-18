package com.asakaa.synthesis.util;

import com.asakaa.synthesis.domain.dto.request.PatientRequest;
import com.asakaa.synthesis.domain.dto.response.PatientResponse;
import com.asakaa.synthesis.domain.entity.Patient;
import org.springframework.stereotype.Component;

@Component
public class PatientMapper {

    public Patient toEntity(PatientRequest request) {
        return Patient.builder()
                .nationalId(request.getNationalId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .bloodGroup(request.getBloodGroup())
                .allergies(request.getAllergies())
                .clinicName(request.getClinicName())
                .region(request.getRegion())
                .build();
    }

    public PatientResponse toResponse(Patient patient) {
        return PatientResponse.builder()
                .id(patient.getId())
                .nationalId(patient.getNationalId())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .dateOfBirth(patient.getDateOfBirth())
                .gender(patient.getGender())
                .bloodGroup(patient.getBloodGroup())
                .allergies(patient.getAllergies())
                .clinicName(patient.getClinicName())
                .region(patient.getRegion())
                .createdAt(patient.getCreatedAt())
                .build();
    }
}
