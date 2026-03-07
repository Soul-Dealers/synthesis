package com.asakaa.synthesis.repository;

import com.asakaa.synthesis.domain.entity.AccessGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccessGrantRepository extends JpaRepository<AccessGrant, Long> {

    // Find access grant by referral
    Optional<AccessGrant> findByReferralId(Long referralId);

    // Find active access grants for a clinic and patient
    @Query("""
        SELECT ag FROM AccessGrant ag
        WHERE ag.clinic.id = :clinicId
        AND ag.patient.id = :patientId
        AND ag.revoked = false
        AND ag.expiresAt > :now
    """)
    List<AccessGrant> findActiveGrantsForClinicAndPatient(
            @Param("clinicId") Long clinicId,
            @Param("patientId") Long patientId,
            @Param("now") LocalDateTime now
    );

    // Find all active grants for a patient
    @Query("""
        SELECT ag FROM AccessGrant ag
        WHERE ag.patient.id = :patientId
        AND ag.revoked = false
        AND ag.expiresAt > :now
    """)
    List<AccessGrant> findActiveGrantsForPatient(
            @Param("patientId") Long patientId,
            @Param("now") LocalDateTime now
    );

    // Find all grants for a clinic (for SUPER_ADMIN oversight)
    List<AccessGrant> findByClinicId(Long clinicId);

    // Check if clinic has active access to patient
    @Query("""
        SELECT CASE WHEN COUNT(ag) > 0 THEN true ELSE false END
        FROM AccessGrant ag
        WHERE ag.clinic.id = :clinicId
        AND ag.patient.id = :patientId
        AND ag.revoked = false
        AND ag.expiresAt > :now
    """)
    boolean hasActiveAccess(
            @Param("clinicId") Long clinicId,
            @Param("patientId") Long patientId,
            @Param("now") LocalDateTime now
    );
}
