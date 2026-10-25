package com.nourtech.ecareplus.care.application.service;

import com.nourtech.ecareplus.alert.application.service.AlerteService;
import com.nourtech.ecareplus.care.application.command.EnregistrerMesureCommand;
import com.nourtech.ecareplus.care.domain.entity.DossierSuivi;
import com.nourtech.ecareplus.care.domain.entity.MesureSante;
import com.nourtech.ecareplus.care.domain.enumeration.StatutDossier;
import com.nourtech.ecareplus.care.infrastructure.persistence.DossierSuiviRepository;
import com.nourtech.ecareplus.care.infrastructure.persistence.MesureSanteRepository;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.infrastructure.persistence.PatientRepository;
import com.nourtech.ecareplus.shared.application.exception.ConflitMetierException;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.Objects;
import java.time.temporal.ChronoUnit;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.LockModeType;

@Service
@Transactional
public class MesureSanteService {

    private final MesureSanteRepository mesureRepository;
    private final DossierSuiviRepository dossierRepository;
    private final PatientRepository patientRepository;
    private final AlerteService alerteService;

    @PersistenceContext
    private EntityManager entityManager;

    public MesureSanteService(
            MesureSanteRepository mesureRepository,
            DossierSuiviRepository dossierRepository,
            PatientRepository patientRepository,
            AlerteService alerteService
    ) {
        this.mesureRepository = mesureRepository;
        this.dossierRepository = dossierRepository;
        this.patientRepository = patientRepository;
        this.alerteService = alerteService;
    }

    public MesureSante enregistrer(EnregistrerMesureCommand commande) {
        DossierSuivi dossier = dossierRepository.findById(commande.dossierId())
                .orElseThrow(() -> new RessourceIntrouvableException("DossierSuivi", commande.dossierId()));
        if (dossier.getStatut() != StatutDossier.ACTIF) {
            throw new ConflitMetierException("Une mesure ne peut être enregistrée que sur un dossier actif");
        }
        Patient patient = patientRepository.findById(commande.patientId())
                .orElseThrow(() -> new RessourceIntrouvableException("Patient", commande.patientId()));
        if (!patient.memeIdentiteQue(dossier.getPatient())) {
            throw new IllegalArgumentException("Le patient ne correspond pas au dossier de suivi");
        }

        MesureSante candidate = new MesureSante(
                patient,
                dossier,
                commande.typeMesure(),
                commande.valeurPrincipale(),
                commande.valeurSecondaire(),
                commande.unite(),
                commande.dateMesure(),
                commande.contexteMesure(),
                commande.commentaire(),
                commande.source(),
                commande.statutSynchronisation()
        );
        candidate.definirSourceRef(commande.sourceRef());
        if (candidate.getSourceRef() != null) {
            // Serialize retries for the same patient; the database unique index is a second guard.
            entityManager.lock(patient, LockModeType.PESSIMISTIC_WRITE);
            MesureSante precedente = mesureRepository.findByPatientIdAndSourceRef(patient.getId(), candidate.getSourceRef()).orElse(null);
            if (precedente != null) {
                if (!memeContenu(precedente, candidate))
                    throw new ConflitMetierException("sourceRef a déjà été utilisée pour une mesure différente");
                return precedente;
            }
        }
        MesureSante mesure = mesureRepository.save(candidate);
        alerteService.evaluerAutomatiquement(mesure);
        return mesure;
    }

    private static boolean memeContenu(MesureSante a, MesureSante b) {
        return a.getDossier().getId().equals(b.getDossier().getId())
                && a.getTypeMesure() == b.getTypeMesure() && a.getSource() == b.getSource()
                && a.getValeurPrincipale().compareTo(b.getValeurPrincipale()) == 0
                && (a.getValeurSecondaire() == null ? b.getValeurSecondaire() == null
                    : b.getValeurSecondaire() != null && a.getValeurSecondaire().compareTo(b.getValeurSecondaire()) == 0)
                && a.getUnite().equals(b.getUnite())
                && a.getDateMesure().truncatedTo(ChronoUnit.MICROS).equals(b.getDateMesure().truncatedTo(ChronoUnit.MICROS))
                && Objects.equals(a.getContexteMesure(), b.getContexteMesure())
                && Objects.equals(a.getCommentaire(), b.getCommentaire());
    }

    public MesureSante modifier(
            UUID mesureId,
            BigDecimal valeurPrincipale,
            BigDecimal valeurSecondaire,
            String unite,
            String contexteMesure,
            String commentaire
    ) {
        MesureSante mesure = charger(mesureId);
        mesure.modifier(valeurPrincipale, valeurSecondaire, unite, contexteMesure, commentaire);
        alerteService.evaluerAutomatiquement(mesure);
        return mesure;
    }

    public MesureSante confirmer(UUID mesureId) {
        MesureSante mesure = charger(mesureId);
        mesure.confirmer();
        return mesure;
    }

    public MesureSante consulter(UUID mesureId) {
        return charger(mesureId);
    }

    private MesureSante charger(UUID mesureId) {
        return mesureRepository.findById(mesureId)
                .orElseThrow(() -> new RessourceIntrouvableException("MesureSante", mesureId));
    }
}
