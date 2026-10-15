package com.nourtech.ecareplus.care.application.service;

import com.nourtech.ecareplus.care.application.command.AjouterObservationCommand;
import com.nourtech.ecareplus.care.domain.entity.DossierSuivi;
import com.nourtech.ecareplus.care.domain.entity.ObservationSuivi;
import com.nourtech.ecareplus.care.infrastructure.persistence.DossierSuiviRepository;
import com.nourtech.ecareplus.care.infrastructure.persistence.ObservationSuiviRepository;
import com.nourtech.ecareplus.identity.domain.entity.ProfessionnelSante;
import com.nourtech.ecareplus.identity.infrastructure.persistence.ProfessionnelSanteRepository;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ObservationSuiviService {

    private final ObservationSuiviRepository observationRepository;
    private final DossierSuiviRepository dossierRepository;
    private final ProfessionnelSanteRepository professionnelRepository;

    public ObservationSuiviService(
            ObservationSuiviRepository observationRepository,
            DossierSuiviRepository dossierRepository,
            ProfessionnelSanteRepository professionnelRepository
    ) {
        this.observationRepository = observationRepository;
        this.dossierRepository = dossierRepository;
        this.professionnelRepository = professionnelRepository;
    }

    public ObservationSuivi ajouter(AjouterObservationCommand commande) {
        DossierSuivi dossier = dossierRepository.findById(commande.dossierId())
                .orElseThrow(() -> new RessourceIntrouvableException("DossierSuivi", commande.dossierId()));
        ProfessionnelSante professionnel = professionnelRepository.findById(commande.professionnelId())
                .orElseThrow(() -> new RessourceIntrouvableException("ProfessionnelSante", commande.professionnelId()));

        return observationRepository.save(new ObservationSuivi(
                dossier,
                professionnel,
                commande.contenu(),
                commande.typeObservation(),
                commande.confidentielle()
        ));
    }

    public ObservationSuivi modifier(UUID observationId, String contenu, String typeObservation, boolean confidentielle) {
        ObservationSuivi observation = charger(observationId);
        observation.modifierObservation(contenu, typeObservation, confidentielle);
        return observation;
    }

    public ObservationSuivi archiver(UUID observationId) {
        ObservationSuivi observation = charger(observationId);
        observation.archiverObservation();
        return observation;
    }

    public ObservationSuivi consulter(UUID observationId) {
        return charger(observationId);
    }

    private ObservationSuivi charger(UUID observationId) {
        return observationRepository.findById(observationId)
                .orElseThrow(() -> new RessourceIntrouvableException("ObservationSuivi", observationId));
    }
}
