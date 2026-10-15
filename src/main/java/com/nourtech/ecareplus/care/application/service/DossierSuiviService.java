package com.nourtech.ecareplus.care.application.service;

import com.nourtech.ecareplus.care.application.command.DefinirObjectifCommand;
import com.nourtech.ecareplus.care.application.command.OuvrirDossierCommand;
import com.nourtech.ecareplus.care.domain.entity.DossierSuivi;
import com.nourtech.ecareplus.care.domain.entity.ObjectifSuivi;
import com.nourtech.ecareplus.care.domain.enumeration.StatutDossier;
import com.nourtech.ecareplus.care.infrastructure.persistence.DossierSuiviRepository;
import com.nourtech.ecareplus.care.infrastructure.persistence.ObjectifSuiviRepository;
import com.nourtech.ecareplus.identity.domain.entity.Medecin;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.infrastructure.persistence.MedecinRepository;
import com.nourtech.ecareplus.identity.infrastructure.persistence.PatientRepository;
import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
import com.nourtech.ecareplus.organization.infrastructure.persistence.StructureSanteRepository;
import com.nourtech.ecareplus.shared.application.exception.ConflitMetierException;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class DossierSuiviService {

    private final DossierSuiviRepository dossierRepository;
    private final ObjectifSuiviRepository objectifRepository;
    private final PatientRepository patientRepository;
    private final StructureSanteRepository structureRepository;
    private final MedecinRepository medecinRepository;

    public DossierSuiviService(
            DossierSuiviRepository dossierRepository,
            ObjectifSuiviRepository objectifRepository,
            PatientRepository patientRepository,
            StructureSanteRepository structureRepository,
            MedecinRepository medecinRepository
    ) {
        this.dossierRepository = dossierRepository;
        this.objectifRepository = objectifRepository;
        this.patientRepository = patientRepository;
        this.structureRepository = structureRepository;
        this.medecinRepository = medecinRepository;
    }

    public DossierSuivi ouvrir(UUID patientId, UUID structureId, String frequenceSuivi) {
        dossierRepository.findFirstByPatientIdAndStructureIdAndStatut(
                patientId,
                structureId,
                StatutDossier.ACTIF
        ).ifPresent(dossier -> {
            throw new ConflitMetierException("Un dossier actif existe déjà pour ce patient dans cette structure");
        });

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RessourceIntrouvableException("Patient", patientId));
        StructureSante structure = structureRepository.findById(structureId)
                .orElseThrow(() -> new RessourceIntrouvableException("StructureSante", structureId));

        return dossierRepository.save(new DossierSuivi(patient, structure, frequenceSuivi));
    }

    public DossierSuivi ouvrir(OuvrirDossierCommand commande) {
        return ouvrir(commande.patientId(), commande.structureId(), commande.frequenceSuivi());
    }

    public ObjectifSuivi definirObjectif(
            UUID dossierId,
            UUID medecinId,
            String libelle,
            String description,
            BigDecimal cibleMin,
            BigDecimal cibleMax,
            String unite,
            LocalDate dateDebut,
            LocalDate dateFin
    ) {
        DossierSuivi dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RessourceIntrouvableException("DossierSuivi", dossierId));
        if (dossier.getStatut() != StatutDossier.ACTIF) {
            throw new ConflitMetierException("Un objectif ne peut être défini que sur un dossier actif");
        }
        Medecin medecin = medecinRepository.findById(medecinId)
                .orElseThrow(() -> new RessourceIntrouvableException("Medecin", medecinId));

        return objectifRepository.save(new ObjectifSuivi(
                dossier,
                medecin,
                libelle,
                description,
                cibleMin,
                cibleMax,
                unite,
                dateDebut,
                dateFin
        ));
    }

    public ObjectifSuivi definirObjectif(DefinirObjectifCommand commande) {
        return definirObjectif(
                commande.dossierId(), commande.medecinId(), commande.libelle(), commande.description(),
                commande.cibleMin(), commande.cibleMax(), commande.unite(), commande.dateDebut(),
                commande.dateFin()
        );
    }

    public DossierSuivi consulter(UUID dossierId) {
        return dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RessourceIntrouvableException("DossierSuivi", dossierId));
    }
}
