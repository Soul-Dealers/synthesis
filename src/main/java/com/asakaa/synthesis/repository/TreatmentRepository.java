package com.asakaa.synthesis.repository;

import com.asakaa.synthesis.domain.entity.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TreatmentRepository extends JpaRepository<Treatment, Long> {

    List<Treatment> findByDiagnosisId(Long diagnosisId);
}
