package com.nourtech.ecareplus.consent.infrastructure.persistence;

import com.nourtech.ecareplus.consent.domain.entity.AutorisationPartage;
import com.nourtech.ecareplus.consent.domain.enumeration.StatutAutorisation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AutorisationPartageRepository extends JpaRepository<AutorisationPartage, UUID> {

    List<AutorisationPartage> findAllByPatientIdAndStatut(UUID patientId, StatutAutorisation statut);

    List<AutorisationPartage> findAllByProcheAidantIdAndStatut(UUID procheAidantId, StatutAutorisation statut);

    List<AutorisationPartage> findAllByConsentementIdAndStatut(UUID consentementId, StatutAutorisation statut);
}
