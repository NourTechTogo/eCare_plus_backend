package com.nourtech.ecareplus.care.application.service;

import com.nourtech.ecareplus.care.domain.entity.DossierSuivi;
import com.nourtech.ecareplus.care.domain.enumeration.StatutDossier;
import com.nourtech.ecareplus.care.infrastructure.persistence.DossierSuiviRepository;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/** Personal dossiers never acquire professional rights by an implicit structure. */
@Service @RequiredArgsConstructor
public class DossierPersonnelService {
    private final DossierSuiviRepository dossiers;
    private final EntityManager em;

    @Transactional
    public DossierSuivi obtenir(UUID patientId) {
        var existant = dossiers.findFirstByPatientIdAndStructureIdIsNullAndStatut(patientId, StatutDossier.ACTIF);
        if (existant.isPresent()) return existant.get();
        Patient patient = em.find(Patient.class, patientId, LockModeType.PESSIMISTIC_WRITE);
        if (patient == null) throw new RessourceIntrouvableException("Patient", patientId);
        return dossiers.findFirstByPatientIdAndStructureIdIsNullAndStatut(patientId, StatutDossier.ACTIF)
                .orElseGet(() -> dossiers.saveAndFlush(DossierSuivi.personnel(patient)));
    }
}
