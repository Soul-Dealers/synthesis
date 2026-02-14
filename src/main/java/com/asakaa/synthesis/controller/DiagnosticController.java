package com.asakaa.synthesis.controller;

import com.asakaa.synthesis.domain.dto.request.DiagnosticRequest;
import com.asakaa.synthesis.domain.dto.response.DiagnosticResponse;
import com.asakaa.synthesis.service.DiagnosticService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/diagnostic")
@RequiredArgsConstructor
public class DiagnosticController {

    private final DiagnosticService diagnosticService;

    @PostMapping("/analyze")
    public ResponseEntity<DiagnosticResponse> analyze(@Valid @RequestBody DiagnosticRequest request) {
        DiagnosticResponse response = diagnosticService.analyze(request);
        return ResponseEntity.ok(response);
    }
}
