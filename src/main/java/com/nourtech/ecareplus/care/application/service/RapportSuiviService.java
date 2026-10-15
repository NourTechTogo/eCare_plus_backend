package com.nourtech.ecareplus.care.application.service;

import com.nourtech.ecareplus.care.application.command.GenererRapportCommand;
import com.nourtech.ecareplus.care.domain.entity.DossierSuivi;
import com.nourtech.ecareplus.care.domain.entity.RapportSuivi;
import com.nourtech.ecareplus.care.infrastructure.persistence.DossierSuiviRepository;
import com.nourtech.ecareplus.care.infrastructure.persistence.RapportSuiviRepository;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class RapportSuiviService {

    private final RapportSuiviRepository rapportRepository;
    private final DossierSuiviRepository dossierRepository;

    public RapportSuiviService(RapportSuiviRepository rapportRepository, DossierSuiviRepository dossierRepository) {
        this.rapportRepository = rapportRepository;
        this.dossierRepository = dossierRepository;
    }

    public RapportSuivi generer(GenererRapportCommand commande) {
        DossierSuivi dossier = dossierRepository.findById(commande.dossierId())
                .orElseThrow(() -> new RessourceIntrouvableException("DossierSuivi", commande.dossierId()));

        return rapportRepository.save(new RapportSuivi(
                dossier,
                commande.titre(),
                commande.dateDebut(),
                commande.dateFin(),
                commande.resume(),
                commande.format()
        ));
    }

    public RapportSuivi marquerGenere(UUID rapportId, String cheminFichier) {
        RapportSuivi rapport = charger(rapportId);
        rapport.marquerGenere(cheminFichier);
        return rapport;
    }

    public RapportSuivi archiver(UUID rapportId) {
        RapportSuivi rapport = charger(rapportId);
        rapport.archiver();
        return rapport;
    }

    public RapportSuivi consulter(UUID rapportId) {
        return charger(rapportId);
    }

    private RapportSuivi charger(UUID rapportId) {
        return rapportRepository.findById(rapportId)
                .orElseThrow(() -> new RessourceIntrouvableException("RapportSuivi", rapportId));
    }
}
