package com.asakaa.synthesis.controller;

import com.asakaa.synthesis.domain.dto.response.OutcomeResponse;
import com.asakaa.synthesis.domain.dto.response.TrendResponse;
import com.asakaa.synthesis.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/trends")
    public ResponseEntity<List<TrendResponse>> getDiseaseTrends(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<TrendResponse> response = analyticsService.getDiseaseTrends(region, from, to);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/outcomes")
    public ResponseEntity<List<OutcomeResponse>> getTreatmentOutcomes() {
        List<OutcomeResponse> response = analyticsService.getTreatmentOutcomes();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/clinic/{clinicName}")
    public ResponseEntity<Map<String, Object>> getClinicSummary(@PathVariable String clinicName) {
        Map<String, Object> response = analyticsService.getClinicSummary(clinicName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        Map<String, Object> response = analyticsService.getDashboardSummary();
        return ResponseEntity.ok(response);
    }
}
