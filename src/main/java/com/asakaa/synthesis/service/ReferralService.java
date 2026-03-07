package com.asakaa.synthesis.service;

import com.asakaa.synthesis.domain.dto.request.ReferralRequest;
import com.asakaa.synthesis.domain.dto.response.AccessGrantResponse;
import com.asakaa.synthesis.domain.dto.response.ReferralResponse;
import com.asakaa.synthesis.domain.entity.*;
import com.asakaa.synthesis.exception.ClinicAccessDeniedException;
import com.asakaa.synthesis.exception.ResourceNotFoundException;
import com.asakaa.synthesis.exception.ValidationException;
import com.asakaa.synthesis.repository.*;
import com.asakaa.synthesis.security.ClinicAccessGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralService {

    private final ReferralRepository referralRepository;
    private final AccessGrantRepository accessGrantRepository;
    private final PatientRepository patientRepository;
    private final ClinicRepository clinicRepository;
    private final ProviderRepository providerRepository;
    private final ClinicAccessGuard clinicAccessGuard;
    private final AuditService auditService;

    @Transactional
    public ReferralResponse createReferral(ReferralRequest request, Authentication authentication) {
        log.info("Creating referral for patient ID: {} to clinic ID: {}", 
                request.getPatientId(), request.getReceivingClinicId());

        Provider referringProvider = clinicAccessGuard.getCurrentProvider(authentication);

        // Validate patient exists and provider has access
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", request.getPatientId()));

        clinicAccessGuard.verifyPatientAccess(authentication, patient);

        // Validate receiving clinic exists
        Clinic receivingClinic = clinicRepository.findById(request.getReceivingClinicId())
                .orElseThrow(() -> new ResourceNotFoundException("Clinic", request.getReceivingClinicId()));

        // Validate not referring to same clinic
        if (referringProvider.getClinic().getId().equals(receivingClinic.getId())) {
            throw new ValidationException("Cannot refer patient to the same clinic");
        }

        // Validate access duration (7-90 days)
        if (request.getAccessDurationDays() < 7 || request.getAccessDurationDays() > 90) {
            throw new ValidationException("Access duration must be between 7 and 90 days");
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(request.getAccessDurationDays());

        // Create referral
        Referral referral = Referral.builder()
                .patient(patient)
                .referringClinic(referringProvider.getClinic())
                .receivingClinic(receivingClinic)
                .referringProvider(referringProvider)
                .reason(request.getReason())
                .notes(request.getNotes())
                .accessExpiresAt(expiresAt)
                .status(ReferralStatus.PENDING)
                .build();

        referral = referralRepository.save(referral);

        // Create access grant
        AccessGrant accessGrant = AccessGrant.builder()
                .patient(patient)
                .clinic(receivingClinic)
                .referral(referral)
                .permissionType(PermissionType.READ_ONLY)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        accessGrantRepository.save(accessGrant);

        // Audit log
        auditService.logAudit(
                AuditAction.CREATE_REFERRAL,
                patient.getId(),
                "Referral",
                referral.getId(),
                String.format("Created referral from %s to %s, expires: %s",
                        referringProvider.getClinic().getName(),
                        receivingClinic.getName(),
                        expiresAt)
        );

        log.info("Referral created with ID: {}", referral.getId());
        return toResponse(referral);
    }

    @Transactional
    public ReferralResponse acceptReferral(Long referralId, Authentication authentication) {
        log.info("Accepting referral ID: {}", referralId);

        Provider acceptingProvider = clinicAccessGuard.getCurrentProvider(authentication);

        Referral referral = referralRepository.findById(referralId)
                .orElseThrow(() -> new ResourceNotFoundException("Referral", referralId));

        // Verify provider is from receiving clinic
        if (!referral.getReceivingClinic().getId().equals(acceptingProvider.getClinic().getId())) {
            throw new ClinicAccessDeniedException("Only providers from the receiving clinic can accept this referral");
        }

        // Verify referral is in PENDING status
        if (referral.getStatus() != ReferralStatus.PENDING) {
            throw new ValidationException("Only pending referrals can be accepted");
        }

        // Check if expired
        if (referral.getAccessExpiresAt().isBefore(LocalDateTime.now())) {
            referral.setStatus(ReferralStatus.EXPIRED);
            referralRepository.save(referral);
            throw new ValidationException("This referral has expired");
        }

        referral.setStatus(ReferralStatus.ACCEPTED);
        referral.setAcceptedAt(LocalDateTime.now());
        referral.setAcceptedByProvider(acceptingProvider);

        referral = referralRepository.save(referral);

        // Audit log
        auditService.logAudit(
                AuditAction.ACCEPT_REFERRAL,
                referral.getPatient().getId(),
                "Referral",
                referral.getId(),
                String.format("Referral accepted by %s at %s",
                        acceptingProvider.getName(),
                        acceptingProvider.getClinic().getName())
        );

        log.info("Referral accepted by provider ID: {}", acceptingProvider.getId());
        return toResponse(referral);
    }

    @Transactional
    public ReferralResponse completeReferral(Long referralId, Authentication authentication) {
        log.info("Completing referral ID: {}", referralId);

        Provider provider = clinicAccessGuard.getCurrentProvider(authentication);

        Referral referral = referralRepository.findById(referralId)
                .orElseThrow(() -> new ResourceNotFoundException("Referral", referralId));

        // Verify provider is from receiving clinic
        if (!referral.getReceivingClinic().getId().equals(provider.getClinic().getId())) {
            throw new ClinicAccessDeniedException("Only providers from the receiving clinic can complete this referral");
        }

        // Verify referral is ACCEPTED
        if (referral.getStatus() != ReferralStatus.ACCEPTED) {
            throw new ValidationException("Only accepted referrals can be completed");
        }

        referral.setStatus(ReferralStatus.COMPLETED);
        referral.setCompletedAt(LocalDateTime.now());

        referral = referralRepository.save(referral);

        // Audit log
        auditService.logAudit(
                AuditAction.COMPLETE_REFERRAL,
                referral.getPatient().getId(),
                "Referral",
                referral.getId(),
                String.format("Referral completed at %s", provider.getClinic().getName())
        );

        log.info("Referral completed");
        return toResponse(referral);
    }

    @Transactional
    public ReferralResponse cancelReferral(Long referralId, String reason, Authentication authentication) {
        log.info("Cancelling referral ID: {}", referralId);

        Provider provider = clinicAccessGuard.getCurrentProvider(authentication);

        Referral referral = referralRepository.findById(referralId)
                .orElseThrow(() -> new ResourceNotFoundException("Referral", referralId));

        // Only referring provider or SUPER_ADMIN can cancel
        if (!referral.getReferringProvider().getId().equals(provider.getId()) 
                && !clinicAccessGuard.isSuperAdmin(authentication)) {
            throw new ClinicAccessDeniedException("Only the referring provider or SUPER_ADMIN can cancel this referral");
        }

        // Can only cancel PENDING or ACCEPTED referrals
        if (referral.getStatus() != ReferralStatus.PENDING && referral.getStatus() != ReferralStatus.ACCEPTED) {
            throw new ValidationException("Only pending or accepted referrals can be cancelled");
        }

        referral.setStatus(ReferralStatus.CANCELLED);
        referral.setCancelledAt(LocalDateTime.now());
        referral.setCancellationReason(reason);

        referral = referralRepository.save(referral);

        // Revoke access grant
        accessGrantRepository.findByReferralId(referralId).ifPresent(grant -> {
            grant.setRevoked(true);
            grant.setRevokedAt(LocalDateTime.now());
            grant.setRevokedBy(provider.getEmail());
            grant.setRevocationReason("Referral cancelled: " + reason);
            accessGrantRepository.save(grant);
        });

        // Audit log
        auditService.logAudit(
                AuditAction.CANCEL_REFERRAL,
                referral.getPatient().getId(),
                "Referral",
                referral.getId(),
                String.format("Referral cancelled by %s. Reason: %s", provider.getName(), reason)
        );

        log.info("Referral cancelled");
        return toResponse(referral);
    }

    public ReferralResponse getReferralById(Long referralId, Authentication authentication) {
        log.info("Fetching referral ID: {}", referralId);

        Provider provider = clinicAccessGuard.getCurrentProvider(authentication);

        Referral referral = referralRepository.findById(referralId)
                .orElseThrow(() -> new ResourceNotFoundException("Referral", referralId));

        // Verify provider has access (from either clinic or SUPER_ADMIN)
        if (!referral.getReferringClinic().getId().equals(provider.getClinic().getId())
                && !referral.getReceivingClinic().getId().equals(provider.getClinic().getId())
                && !clinicAccessGuard.isSuperAdmin(authentication)) {
            throw new ClinicAccessDeniedException("You do not have access to this referral");
        }

        return toResponse(referral);
    }

    public Page<ReferralResponse> getReferralsByPatient(Long patientId, Pageable pageable, Authentication authentication) {
        log.info("Fetching referrals for patient ID: {}", patientId);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        clinicAccessGuard.verifyPatientAccess(authentication, patient);

        return referralRepository.findByPatientId(patientId, pageable)
                .map(this::toResponse);
    }

    public Page<ReferralResponse> getSentReferrals(Pageable pageable, Authentication authentication) {
        Provider provider = clinicAccessGuard.getCurrentProvider(authentication);
        return referralRepository.findByReferringClinicId(provider.getClinic().getId(), pageable)
                .map(this::toResponse);
    }

    public Page<ReferralResponse> getReceivedReferrals(Pageable pageable, Authentication authentication) {
        Provider provider = clinicAccessGuard.getCurrentProvider(authentication);
        return referralRepository.findByReceivingClinicId(provider.getClinic().getId(), pageable)
                .map(this::toResponse);
    }

    public boolean hasAccessToPatient(Long clinicId, Long patientId) {
        return accessGrantRepository.hasActiveAccess(clinicId, patientId, LocalDateTime.now());
    }

    public List<AccessGrantResponse> getActiveAccessGrants(Long patientId, Authentication authentication) {
        log.info("Fetching active access grants for patient ID: {}", patientId);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        clinicAccessGuard.verifyPatientAccess(authentication, patient);

        return accessGrantRepository.findActiveGrantsForPatient(patientId, LocalDateTime.now())
                .stream()
                .map(this::toAccessGrantResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    public void expireOldReferrals() {
        log.info("Running scheduled task to expire old referrals");

        List<Referral> expiredReferrals = referralRepository.findExpiredReferrals(LocalDateTime.now());

        for (Referral referral : expiredReferrals) {
            referral.setStatus(ReferralStatus.EXPIRED);
            referralRepository.save(referral);

            log.debug("Expired referral ID: {}", referral.getId());
        }

        log.info("Expired {} referrals", expiredReferrals.size());
    }

    private ReferralResponse toResponse(Referral referral) {
        return ReferralResponse.builder()
                .id(referral.getId())
                .patientId(referral.getPatient().getId())
                .patientName(referral.getPatient().getFirstName() + " " + referral.getPatient().getLastName())
                .referringClinicId(referral.getReferringClinic().getId())
                .referringClinicName(referral.getReferringClinic().getName())
                .receivingClinicId(referral.getReceivingClinic().getId())
                .receivingClinicName(referral.getReceivingClinic().getName())
                .referringProviderId(referral.getReferringProvider().getId())
                .referringProviderName(referral.getReferringProvider().getName())
                .reason(referral.getReason())
                .notes(referral.getNotes())
                .accessExpiresAt(referral.getAccessExpiresAt())
                .status(referral.getStatus())
                .acceptedAt(referral.getAcceptedAt())
                .acceptedByProviderName(referral.getAcceptedByProvider() != null 
                        ? referral.getAcceptedByProvider().getName() : null)
                .completedAt(referral.getCompletedAt())
                .cancelledAt(referral.getCancelledAt())
                .cancellationReason(referral.getCancellationReason())
                .createdAt(referral.getCreatedAt())
                .build();
    }

    private AccessGrantResponse toAccessGrantResponse(AccessGrant grant) {
        return AccessGrantResponse.builder()
                .id(grant.getId())
                .patientId(grant.getPatient().getId())
                .patientName(grant.getPatient().getFirstName() + " " + grant.getPatient().getLastName())
                .clinicId(grant.getClinic().getId())
                .clinicName(grant.getClinic().getName())
                .referralId(grant.getReferral().getId())
                .permissionType(grant.getPermissionType())
                .expiresAt(grant.getExpiresAt())
                .revoked(grant.getRevoked())
                .revokedAt(grant.getRevokedAt())
                .revokedBy(grant.getRevokedBy())
                .revocationReason(grant.getRevocationReason())
                .createdAt(grant.getCreatedAt())
                .build();
    }
}
