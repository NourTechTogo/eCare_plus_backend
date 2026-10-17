package com.nourtech.ecareplus.alert.application.service;

import com.nourtech.ecareplus.alert.application.command.GenererAlerteCommand;
import com.nourtech.ecareplus.alert.domain.entity.Alerte;
import com.nourtech.ecareplus.alert.domain.entity.RegleAlerte;
import com.nourtech.ecareplus.alert.domain.enumeration.OrigineAlerte;
import com.nourtech.ecareplus.alert.domain.enumeration.StatutAlerte;
import com.nourtech.ecareplus.alert.domain.enumeration.TypeAlerte;
import com.nourtech.ecareplus.alert.infrastructure.persistence.AlerteRepository;
import com.nourtech.ecareplus.alert.infrastructure.persistence.RegleAlerteRepository;
import com.nourtech.ecareplus.care.domain.entity.MesureSante;
import com.nourtech.ecareplus.care.infrastructure.persistence.MesureSanteRepository;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.domain.entity.ProfessionnelSante;
import com.nourtech.ecareplus.identity.infrastructure.persistence.PatientRepository;
import com.nourtech.ecareplus.identity.infrastructure.persistence.ProfessionnelSanteRepository;
import com.nourtech.ecareplus.shared.application.exception.ConflitMetierException;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AlerteService {

    private final AlerteRepository alerteRepository;
    private final PatientRepository patientRepository;
    private final MesureSanteRepository mesureSanteRepository;
    private final RegleAlerteRepository regleAlerteRepository;
    private final ProfessionnelSanteRepository professionnelRepository;

    public AlerteService(
            AlerteRepository alerteRepository,
            PatientRepository patientRepository,
            MesureSanteRepository mesureSanteRepository,
            RegleAlerteRepository regleAlerteRepository,
            ProfessionnelSanteRepository professionnelRepository
    ) {
        this.alerteRepository = alerteRepository;
        this.patientRepository = patientRepository;
        this.mesureSanteRepository = mesureSanteRepository;
        this.regleAlerteRepository = regleAlerteRepository;
        this.professionnelRepository = professionnelRepository;
    }

    public Alerte generer(GenererAlerteCommand commande) {
        Patient patient = patientRepository.findById(commande.patientId())
                .orElseThrow(() -> new RessourceIntrouvableException("Patient", commande.patientId()));

        MesureSante mesure = null;
        if (commande.mesureId() != null) {
            mesure = mesureSanteRepository.findById(commande.mesureId())
                    .orElseThrow(() -> new RessourceIntrouvableException("MesureSante", commande.mesureId()));
            if (!patient.memeIdentiteQue(mesure.getPatient())) {
                throw new IllegalArgumentException("La mesure n'appartient pas au patient de l'alerte");
            }
        }

        RegleAlerte regle = null;
        if (commande.regleId() != null) {
            regle = regleAlerteRepository.findById(commande.regleId())
                    .orElseThrow(() -> new RessourceIntrouvableException("RegleAlerte", commande.regleId()));
            if (mesure == null) {
                throw new IllegalArgumentException("Une règle ne peut être liée à une alerte manuelle sans mesure");
            }
            if (regle.getTypeMesure() != mesure.getTypeMesure()) {
                throw new IllegalArgumentException("La règle ne correspond pas au type de la mesure");
            }
            if (alerteRepository.existsByRegleIdAndMesureId(regle.getId(), mesure.getId())) {
                throw new ConflitMetierException("Une alerte existe déjà pour cette règle et cette mesure");
            }
        }

        Alerte alerte = alerteRepository.save(new Alerte(
                patient,
                mesure,
                regle,
                commande.typeAlerte(),
                commande.titre(),
                commande.description(),
                commande.niveauPriorite()
        ));
        return alerte;
    }

    /**
     * Évalue toutes les règles actives correspondant à une nouvelle mesure.
     * La période d'observation d'une règle est exprimée en minutes.
     */
    public List<Alerte> evaluerAutomatiquement(MesureSante mesure) {
        return regleAlerteRepository.findAllByTypeMesureAndActifTrue(mesure.getTypeMesure()).stream()
                .filter(regle -> doitDeclencher(regle, mesure))
                .filter(regle -> !alerteRepository.existsByRegleIdAndMesureId(regle.getId(), mesure.getId()))
                .map(regle -> creerAlerteAutomatique(regle, mesure))
                .toList();
    }

    private boolean doitDeclencher(RegleAlerte regle, MesureSante mesure) {
        Instant debut = mesure.getDateMesure().minus(Duration.ofMinutes(regle.getPeriodeObservation()));
        int occurrences = (int) mesureSanteRepository
                .findAllByPatientIdAndTypeMesureAndDateMesureBetween(
                        mesure.getPatient().getId(), mesure.getTypeMesure(), debut, mesure.getDateMesure()
                ).stream()
                .filter(candidate -> regle.depasseSeuil(candidate.getValeurPrincipale()))
                .count();
        return regle.evaluerMesure(mesure.getValeurPrincipale(), occurrences);
    }

    private Alerte creerAlerteAutomatique(RegleAlerte regle, MesureSante mesure) {
        return alerteRepository.save(new Alerte(
                mesure.getPatient(),
                mesure,
                regle,
                TypeAlerte.SEUIL_MESURE,
                regle.getNom(),
                "Une règle de suivi validée a été déclenchée par une mesure.",
                regle.getNiveauPriorite(),
                OrigineAlerte.AUTOMATIQUE
        ));
    }

    public Alerte affecter(UUID alerteId, UUID professionnelId) {
        Alerte alerte = charger(alerteId);
        ProfessionnelSante professionnel = professionnelRepository.findById(professionnelId)
                .orElseThrow(() -> new RessourceIntrouvableException("ProfessionnelSante", professionnelId));
        alerte.affecter(professionnel);
        return alerte;
    }

    public Alerte prendreEnCharge(UUID alerteId, Instant maintenant) {
        Alerte alerte = charger(alerteId);
        alerte.prendreEnCharge(maintenant);
        return alerte;
    }

    public Alerte transmettre(UUID alerteId, UUID nouveauProfessionnelId) {
        Alerte alerte = charger(alerteId);
        ProfessionnelSante professionnel = professionnelRepository.findById(nouveauProfessionnelId)
                .orElseThrow(() -> new RessourceIntrouvableException("ProfessionnelSante", nouveauProfessionnelId));
        alerte.transmettre(professionnel);
        return alerte;
    }

    public Alerte cloturer(UUID alerteId, String motifCloture, Instant maintenant) {
        Alerte alerte = charger(alerteId);
        alerte.cloturer(motifCloture, maintenant);
        return alerte;
    }

    public Alerte rouvrir(UUID alerteId) {
        Alerte alerte = charger(alerteId);
        alerte.rouvrir();
        return alerte;
    }

    public Alerte consulter(UUID alerteId) {
        return charger(alerteId);
    }

    @Transactional(readOnly = true)
    public List<Alerte> listerPourPatient(UUID patientId, StatutAlerte statut) {
        if (!patientRepository.existsById(patientId)) {
            throw new RessourceIntrouvableException("Patient", patientId);
        }
        return alerteRepository.findAllByPatientIdAndStatutOrderByDateCreationDesc(patientId, statut);
    }

    private Alerte charger(UUID alerteId) {
        return alerteRepository.findById(alerteId)
                .orElseThrow(() -> new RessourceIntrouvableException("Alerte", alerteId));
    }
}
