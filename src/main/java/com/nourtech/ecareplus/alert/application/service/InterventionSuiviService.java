package com.nourtech.ecareplus.alert.application.service;

import com.nourtech.ecareplus.alert.application.command.EnregistrerInterventionCommand;
import com.nourtech.ecareplus.alert.domain.entity.Alerte;
import com.nourtech.ecareplus.alert.domain.entity.InterventionSuivi;
import com.nourtech.ecareplus.alert.infrastructure.persistence.AlerteRepository;
import com.nourtech.ecareplus.alert.infrastructure.persistence.InterventionSuiviRepository;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class InterventionSuiviService {

    private final InterventionSuiviRepository interventionRepository;
    private final AlerteRepository alerteRepository;

    public InterventionSuiviService(InterventionSuiviRepository interventionRepository, AlerteRepository alerteRepository) {
        this.interventionRepository = interventionRepository;
        this.alerteRepository = alerteRepository;
    }

    public InterventionSuivi enregistrer(EnregistrerInterventionCommand commande) {
        Alerte alerte = alerteRepository.findById(commande.alerteId())
                .orElseThrow(() -> new RessourceIntrouvableException("Alerte", commande.alerteId()));

        return interventionRepository.save(new InterventionSuivi(
                alerte,
                commande.typeIntervention(),
                commande.description(),
                commande.dateIntervention()
        ));
    }

    public InterventionSuivi modifier(UUID interventionId, String description, String resultat) {
        InterventionSuivi intervention = charger(interventionId);
        intervention.modifierIntervention(description, resultat);
        return intervention;
    }

    public InterventionSuivi definirActionSuivante(UUID interventionId, String actionSuivante, Instant dateProchaineAction) {
        InterventionSuivi intervention = charger(interventionId);
        intervention.definirActionSuivante(actionSuivante, dateProchaineAction);
        return intervention;
    }

    public InterventionSuivi cloturer(UUID interventionId, String resultatFinal) {
        InterventionSuivi intervention = charger(interventionId);
        intervention.cloturerIntervention(resultatFinal);
        return intervention;
    }

    public InterventionSuivi consulter(UUID interventionId) {
        return charger(interventionId);
    }

    private InterventionSuivi charger(UUID interventionId) {
        return interventionRepository.findById(interventionId)
                .orElseThrow(() -> new RessourceIntrouvableException("InterventionSuivi", interventionId));
    }
}
