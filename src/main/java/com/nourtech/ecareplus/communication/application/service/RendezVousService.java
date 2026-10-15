package com.nourtech.ecareplus.communication.application.service;

import com.nourtech.ecareplus.communication.application.command.ProgrammerRendezVousCommand;
import com.nourtech.ecareplus.communication.domain.entity.RendezVous;
import com.nourtech.ecareplus.communication.infrastructure.persistence.RendezVousRepository;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.infrastructure.persistence.PatientRepository;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class RendezVousService {

    private final RendezVousRepository rendezVousRepository;
    private final PatientRepository patientRepository;

    public RendezVousService(RendezVousRepository rendezVousRepository, PatientRepository patientRepository) {
        this.rendezVousRepository = rendezVousRepository;
        this.patientRepository = patientRepository;
    }

    public RendezVous programmer(ProgrammerRendezVousCommand commande) {
        Patient patient = patientRepository.findById(commande.patientId())
                .orElseThrow(() -> new RessourceIntrouvableException("Patient", commande.patientId()));

        return rendezVousRepository.save(new RendezVous(
                patient,
                commande.motif(),
                commande.dateHeure(),
                commande.lieu(),
                commande.typeRendezVous(),
                commande.commentaire()
        ));
    }

    public RendezVous confirmer(UUID rendezVousId) {
        RendezVous rendezVous = charger(rendezVousId);
        rendezVous.confirmer();
        return rendezVous;
    }

    public RendezVous reporter(UUID rendezVousId, Instant nouvelleDateHeure) {
        RendezVous rendezVous = charger(rendezVousId);
        rendezVous.reporter(nouvelleDateHeure);
        return rendezVous;
    }

    public RendezVous annuler(UUID rendezVousId) {
        RendezVous rendezVous = charger(rendezVousId);
        rendezVous.annuler();
        return rendezVous;
    }

    public RendezVous marquerCommeRealise(UUID rendezVousId) {
        RendezVous rendezVous = charger(rendezVousId);
        rendezVous.marquerCommeRealise();
        return rendezVous;
    }

    public RendezVous consulter(UUID rendezVousId) {
        return charger(rendezVousId);
    }

    private RendezVous charger(UUID rendezVousId) {
        return rendezVousRepository.findById(rendezVousId)
                .orElseThrow(() -> new RessourceIntrouvableException("RendezVous", rendezVousId));
    }
}
