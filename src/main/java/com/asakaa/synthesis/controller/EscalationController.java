package com.asakaa.synthesis.controller;

import com.asakaa.synthesis.domain.dto.request.EscalationRequest;
import com.asakaa.synthesis.domain.dto.response.EscalationResponse;
import com.asakaa.synthesis.service.EscalationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/escalation")
@RequiredArgsConstructor
public class EscalationController {

    private final EscalationService escalationService;

    @PostMapping("/refer")
    public ResponseEntity<EscalationResponse> escalate(@Valid @RequestBody EscalationRequest request) {
        EscalationResponse response = escalationService.escalate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EscalationResponse> getEscalationById(@PathVariable Long id) {
        EscalationResponse response = escalationService.getEscalationById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/consultation/{consultationId}")
    public ResponseEntity<List<EscalationResponse>> getEscalationsByConsultation(
            @PathVariable Long consultationId) {
        List<EscalationResponse> response = escalationService.getEscalationsByConsultation(consultationId);
        return ResponseEntity.ok(response);
    }
}
