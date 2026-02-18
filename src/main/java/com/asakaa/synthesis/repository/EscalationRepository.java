package com.asakaa.synthesis.repository;

import com.asakaa.synthesis.domain.entity.Escalation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EscalationRepository extends JpaRepository<Escalation, Long> {

    List<Escalation> findByConsultationId(Long consultationId);

    List<Escalation> findByStatus(String status);

    Optional<Escalation> findByReferralId(String referralId);
}
