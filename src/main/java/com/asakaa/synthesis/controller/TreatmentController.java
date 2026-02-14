package com.asakaa.synthesis.controller;

import com.asakaa.synthesis.domain.dto.request.TreatmentRequest;
import com.asakaa.synthesis.domain.dto.response.TreatmentPlanResponse;
import com.asakaa.synthesis.domain.dto.response.TreatmentResponse;
import com.asakaa.synthesis.service.TreatmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/diagnostic")
@RequiredArgsConstructor
public class TreatmentController {

    private final TreatmentService treatmentService;

    @PostMapping("/treatment")
    public ResponseEntity<TreatmentPlanResponse> generateTreatmentPlan(
            @Valid @RequestBody TreatmentRequest request) {
        TreatmentPlanResponse response = treatmentService.generateTreatmentPlan(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/treatment/{diagnosisId}")
    public ResponseEntity<List<TreatmentResponse>> getTreatmentsByDiagnosis(@PathVariable Long diagnosisId) {
        List<TreatmentResponse> response = treatmentService.getTreatmentsByDiagnosis(diagnosisId);
        return ResponseEntity.ok(response);
    }
}
