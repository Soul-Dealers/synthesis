package com.asakaa.synthesis.util;

import com.asakaa.synthesis.domain.dto.request.PatientRequest;
import com.asakaa.synthesis.domain.dto.response.PatientResponse;
import com.asakaa.synthesis.domain.entity.Clinic;
import com.asakaa.synthesis.domain.entity.Patient;
import com.asakaa.synthesis.repository.ClinicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PatientMapper {

    private final ClinicRepository clinicRepository;

    public Patient toEntity(PatientRequest request) {
        Clinic clinic = null;
        if (request.getClinicId() != null) {
            clinic = clinicRepository.findById(request.getClinicId()).orElse(null);
        }

        return Patient.builder()
                .nationalId(request.getNationalId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .bloodGroup(request.getBloodGroup())
                .allergies(request.getAllergies())
                .clinic(clinic)
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
                .clinicId(patient.getClinic() != null ? patient.getClinic().getId() : null)
                .clinicName(patient.getClinic() != null ? patient.getClinic().getName() : null)
                .region(patient.getRegion())
                .createdAt(patient.getCreatedAt())
                .build();
    }
}

