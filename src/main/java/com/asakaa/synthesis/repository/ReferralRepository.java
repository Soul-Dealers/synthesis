package com.asakaa.synthesis.repository;

import com.asakaa.synthesis.domain.entity.Referral;
import com.asakaa.synthesis.domain.entity.ReferralStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, Long> {

    // Find referrals by patient
    Page<Referral> findByPatientId(Long patientId, Pageable pageable);

    // Find referrals sent by a clinic
    Page<Referral> findByReferringClinicId(Long clinicId, Pageable pageable);

    // Find referrals received by a clinic
    Page<Referral> findByReceivingClinicId(Long clinicId, Pageable pageable);

    // Find referrals by status
    Page<Referral> findByStatus(ReferralStatus status, Pageable pageable);

    // Find referrals by referring provider
    Page<Referral> findByReferringProviderId(Long providerId, Pageable pageable);

    // Find active referrals for a patient at a specific clinic
    @Query("""
        SELECT r FROM Referral r
        WHERE r.patient.id = :patientId
        AND r.receivingClinic.id = :clinicId
        AND r.status IN ('PENDING', 'ACCEPTED')
        AND r.accessExpiresAt > :now
    """)
    List<Referral> findActiveReferralsForPatientAtClinic(
            @Param("patientId") Long patientId,
            @Param("clinicId") Long clinicId,
            @Param("now") LocalDateTime now
    );

    // Find expired referrals that need status update
    @Query("""
        SELECT r FROM Referral r
        WHERE r.status IN ('PENDING', 'ACCEPTED')
        AND r.accessExpiresAt <= :now
    """)
    List<Referral> findExpiredReferrals(@Param("now") LocalDateTime now);

    // Count active referrals for a patient
    @Query("""
        SELECT COUNT(r) FROM Referral r
        WHERE r.patient.id = :patientId
        AND r.status IN ('PENDING', 'ACCEPTED')
        AND r.accessExpiresAt > :now
    """)
    Long countActiveReferralsForPatient(
            @Param("patientId") Long patientId,
            @Param("now") LocalDateTime now
    );
}
