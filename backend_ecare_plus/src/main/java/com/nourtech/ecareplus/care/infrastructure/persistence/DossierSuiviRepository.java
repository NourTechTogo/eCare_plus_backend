package com.nourtech.ecareplus.care.infrastructure.persistence;

import com.nourtech.ecareplus.care.domain.entity.DossierSuivi;
import com.nourtech.ecareplus.care.domain.enumeration.StatutDossier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DossierSuiviRepository extends JpaRepository<DossierSuivi, UUID> {

    Optional<DossierSuivi> findFirstByPatientIdAndStructureIdAndStatut(
            UUID patientId,
            UUID structureId,
            StatutDossier statut
    );

    Optional<DossierSuivi> findFirstByPatientIdAndStructureIdIsNullAndStatut(UUID patientId, StatutDossier statut);

    List<DossierSuivi> findAllByPatientId(UUID patientId);

    Optional<DossierSuivi> findFirstByPatientIdAndStatutOrderByDateOuvertureDesc(
            UUID patientId,
            StatutDossier statut
    );
}
