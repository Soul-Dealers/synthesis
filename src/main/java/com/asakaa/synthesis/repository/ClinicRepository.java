package com.asakaa.synthesis.repository;

import com.asakaa.synthesis.domain.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClinicRepository extends JpaRepository<Clinic, Long> {

    Optional<Clinic> findByRegistrationCode(String registrationCode);

    Optional<Clinic> findByName(String name);

    boolean existsByName(String name);

    boolean existsByRegistrationCode(String registrationCode);
}
