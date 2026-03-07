package com.asakaa.synthesis.controller;

import com.asakaa.synthesis.domain.dto.request.ReferralRequest;
import com.asakaa.synthesis.domain.dto.response.AccessGrantResponse;
import com.asakaa.synthesis.domain.dto.response.ReferralResponse;
import com.asakaa.synthesis.service.ReferralService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    @PostMapping
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ReferralResponse> createReferral(
            @Valid @RequestBody ReferralRequest request,
            Authentication authentication) {
        log.info("POST /api/v1/referrals - Creating referral");
        ReferralResponse response = referralService.createReferral(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ReferralResponse> acceptReferral(
            @PathVariable Long id,
            Authentication authentication) {
        log.info("PUT /api/v1/referrals/{}/accept - Accepting referral", id);
        ReferralResponse response = referralService.acceptReferral(id, authentication);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ReferralResponse> completeReferral(
            @PathVariable Long id,
            Authentication authentication) {
        log.info("PUT /api/v1/referrals/{}/complete - Completing referral", id);
        ReferralResponse response = referralService.completeReferral(id, authentication);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ReferralResponse> cancelReferral(
            @PathVariable Long id,
            @RequestParam String reason,
            Authentication authentication) {
        log.info("PUT /api/v1/referrals/{}/cancel - Cancelling referral", id);
        ReferralResponse response = referralService.cancelReferral(id, reason, authentication);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ReferralResponse> getReferralById(
            @PathVariable Long id,
            Authentication authentication) {
        log.info("GET /api/v1/referrals/{} - Fetching referral", id);
        ReferralResponse response = referralService.getReferralById(id, authentication);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Page<ReferralResponse>> getReferralsByPatient(
            @PathVariable Long patientId,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        log.info("GET /api/v1/referrals/patient/{} - Fetching patient referrals", patientId);
        Page<ReferralResponse> response = referralService.getReferralsByPatient(patientId, pageable, authentication);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sent")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Page<ReferralResponse>> getSentReferrals(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        log.info("GET /api/v1/referrals/sent - Fetching sent referrals");
        Page<ReferralResponse> response = referralService.getSentReferrals(pageable, authentication);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/received")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Page<ReferralResponse>> getReceivedReferrals(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        log.info("GET /api/v1/referrals/received - Fetching received referrals");
        Page<ReferralResponse> response = referralService.getReceivedReferrals(pageable, authentication);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patient/{patientId}/access-grants")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<AccessGrantResponse>> getActiveAccessGrants(
            @PathVariable Long patientId,
            Authentication authentication) {
        log.info("GET /api/v1/referrals/patient/{}/access-grants - Fetching active access grants", patientId);
        List<AccessGrantResponse> response = referralService.getActiveAccessGrants(patientId, authentication);
        return ResponseEntity.ok(response);
    }
}
