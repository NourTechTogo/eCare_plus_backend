package com.nourtech.ecareplus.organization.infrastructure.persistence;

import com.nourtech.ecareplus.organization.domain.entity.AffectationPatient;
import com.nourtech.ecareplus.organization.domain.enumeration.StatutAffectation;
import com.nourtech.ecareplus.organization.domain.enumeration.RoleProfessionnel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AffectationPatientRepository extends JpaRepository<AffectationPatient, UUID> {

    List<AffectationPatient> findAllByPatientIdAndStatut(UUID patientId, StatutAffectation statut);

    List<AffectationPatient> findAllByProfessionnelIdAndStatut(UUID professionnelId, StatutAffectation statut);

    boolean existsByPatientIdAndProfessionnelIdAndStatut(
            UUID patientId,
            UUID professionnelId,
            StatutAffectation statut
    );

    List<AffectationPatient> findAllByPatientIdAndStructureIdAndRoleProfessionnelAndStatut(
            UUID patientId,
            UUID structureId,
            RoleProfessionnel roleProfessionnel,
            StatutAffectation statut
    );
}
