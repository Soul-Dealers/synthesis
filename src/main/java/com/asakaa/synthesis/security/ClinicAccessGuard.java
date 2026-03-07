package com.asakaa.synthesis.security;

import com.asakaa.synthesis.domain.entity.Consultation;
import com.asakaa.synthesis.domain.entity.Patient;
import com.asakaa.synthesis.domain.entity.Provider;
import com.asakaa.synthesis.exception.ClinicAccessDeniedException;
import com.asakaa.synthesis.repository.AccessGrantRepository;
import com.asakaa.synthesis.repository.ProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ClinicAccessGuard {

    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";
    private final ProviderRepository providerRepository;
    private final AccessGrantRepository accessGrantRepository;

    /**
     * Resolves the authenticated provider from the security context.
     */
    public Provider getCurrentProvider(Authentication authentication) {
        String email = authentication.getName();
        return providerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated provider not found: " + email));
    }

    /**
     * Returns true if the authenticated user has the SUPER_ADMIN role.
     */
    public boolean isSuperAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + SUPER_ADMIN_ROLE));
    }

    /**
     * Verifies the authenticated provider has access to the given patient.
     * Access is granted if:
     * 1. User is SUPER_ADMIN
     * 2. Patient belongs to provider's clinic
     * 3. Provider's clinic has an active referral-based access grant
     */
    public void verifyPatientAccess(Authentication authentication, Patient patient) {
        if (isSuperAdmin(authentication)) {
            return;
        }

        Provider provider = getCurrentProvider(authentication);

        if (provider.getClinic() == null) {
            throw new ClinicAccessDeniedException("Provider is not associated with any clinic");
        }

        if (patient.getClinic() == null) {
            throw new ClinicAccessDeniedException("Patient is not associated with any clinic");
        }

        // Check if patient belongs to provider's clinic
        if (provider.getClinic().getId().equals(patient.getClinic().getId())) {
            return; // Direct access granted
        }

        // Check if provider's clinic has active referral-based access
        boolean hasReferralAccess = accessGrantRepository.hasActiveAccess(
                provider.getClinic().getId(),
                patient.getId(),
                LocalDateTime.now()
        );

        if (!hasReferralAccess) {
            throw new ClinicAccessDeniedException(
                    "Access denied: patient belongs to a different clinic and no active referral exists");
        }
    }

    /**
     * Verifies the authenticated provider has access to the given consultation
     * by checking the consultation's patient clinic.
     */
    public void verifyConsultationAccess(Authentication authentication, Consultation consultation) {
        verifyPatientAccess(authentication, consultation.getPatient());
    }
}
