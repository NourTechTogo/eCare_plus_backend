package com.nourtech.ecareplus.identity.infrastructure.persistence;

import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.domain.enumeration.StatutSuivi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByNumeroPatient(String numeroPatient);

    boolean existsByNumeroPatient(String numeroPatient);

    List<Patient> findAllByStatutSuivi(StatutSuivi statutSuivi);
}
