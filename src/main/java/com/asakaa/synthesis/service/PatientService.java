package com.asakaa.synthesis.service;

import com.asakaa.synthesis.domain.dto.request.PatientRequest;
import com.asakaa.synthesis.domain.dto.response.PatientResponse;
import com.asakaa.synthesis.domain.entity.Patient;
import com.asakaa.synthesis.exception.ResourceNotFoundException;
import com.asakaa.synthesis.exception.ValidationException;
import com.asakaa.synthesis.repository.PatientRepository;
import com.asakaa.synthesis.util.PatientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @Transactional
    public PatientResponse createPatient(PatientRequest request) {
        log.info("Creating patient with national ID: {}", request.getNationalId());

        if (patientRepository.existsByNationalId(request.getNationalId())) {
            throw new ValidationException("Patient with national ID " + request.getNationalId() + " already exists");
        }

        Patient patient = patientMapper.toEntity(request);
        patient = patientRepository.save(patient);

        log.info("Patient created successfully with ID: {}", patient.getId());
        return patientMapper.toResponse(patient);
    }

    public PatientResponse getPatientById(Long id) {
        log.info("Fetching patient with ID: {}", id);

        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", id));

        return patientMapper.toResponse(patient);
    }

    @Transactional
    public PatientResponse updatePatient(Long id, PatientRequest request) {
        log.info("Updating patient with ID: {}", id);

        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", id));

        // Check if nationalId is being changed to an existing one
        if (!patient.getNationalId().equals(request.getNationalId()) &&
                patientRepository.existsByNationalId(request.getNationalId())) {
            throw new ValidationException("Patient with national ID " + request.getNationalId() + " already exists");
        }

        patient.setNationalId(request.getNationalId());
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setBloodGroup(request.getBloodGroup());
        patient.setAllergies(request.getAllergies());
        patient.setClinicName(request.getClinicName());
        patient.setRegion(request.getRegion());

        patient = patientRepository.save(patient);

        log.info("Patient updated successfully with ID: {}", patient.getId());
        return patientMapper.toResponse(patient);
    }

    public Page<PatientResponse> getAllPatients(Pageable pageable) {
        log.info("Fetching all patients, page: {}", pageable.getPageNumber());

        return patientRepository.findAll(pageable)
                .map(patientMapper::toResponse);
    }

    public Page<PatientResponse> searchPatients(String query, Pageable pageable) {
        log.info("Searching patients with query: {}", query);

        return patientRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                        query, query, pageable)
                .map(patientMapper::toResponse);
    }
}
