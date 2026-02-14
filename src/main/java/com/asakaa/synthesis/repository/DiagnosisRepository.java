package com.asakaa.synthesis.repository;

import com.asakaa.synthesis.domain.entity.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {

    List<Diagnosis> findByConsultationId(Long consultationId);

    List<Diagnosis> findByConditionNameAndConfidenceScoreGreaterThan(
            String conditionName, BigDecimal threshold);
}
