package com.asakaa.synthesis.controller;

import com.asakaa.synthesis.domain.dto.request.ClinicRegistrationRequest;
import com.asakaa.synthesis.domain.dto.response.AuthResponse;
import com.asakaa.synthesis.domain.dto.response.ClinicResponse;
import com.asakaa.synthesis.service.ClinicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clinics")
@RequiredArgsConstructor
public class ClinicController {

    private final ClinicService clinicService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerClinic(
            @Valid @RequestBody ClinicRegistrationRequest request) {
        AuthResponse response = clinicService.registerClinic(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{clinicId}")
    public ResponseEntity<ClinicResponse> getClinic(@PathVariable Long clinicId) {
        ClinicResponse response = clinicService.getClinicById(clinicId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{clinicId}/staff")
    public ResponseEntity<ClinicResponse> getClinicStaff(@PathVariable Long clinicId) {
        ClinicResponse response = clinicService.getClinicStaff(clinicId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ClinicResponse>> getAllClinics() {
        List<ClinicResponse> response = clinicService.getAllClinics();
        return ResponseEntity.ok(response);
    }
}
