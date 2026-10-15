package com.nourtech.ecareplus.care.application.service;

import com.nourtech.ecareplus.care.application.command.DeclarerPathologieCommand;
import com.nourtech.ecareplus.care.domain.entity.DossierSuivi;
import com.nourtech.ecareplus.care.domain.entity.Pathologie;
import com.nourtech.ecareplus.care.domain.enumeration.NiveauRisque;
import com.nourtech.ecareplus.care.domain.enumeration.StatutDossier;
import com.nourtech.ecareplus.care.infrastructure.persistence.DossierSuiviRepository;
import com.nourtech.ecareplus.care.infrastructure.persistence.PathologieRepository;
import com.nourtech.ecareplus.shared.application.exception.ConflitMetierException;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class PathologieService {

    private final PathologieRepository pathologieRepository;
    private final DossierSuiviRepository dossierRepository;

    public PathologieService(PathologieRepository pathologieRepository, DossierSuiviRepository dossierRepository) {
        this.pathologieRepository = pathologieRepository;
        this.dossierRepository = dossierRepository;
    }

    public Pathologie declarer(DeclarerPathologieCommand commande) {
        DossierSuivi dossier = dossierRepository.findById(commande.dossierId())
                .orElseThrow(() -> new RessourceIntrouvableException("DossierSuivi", commande.dossierId()));
        if (dossier.getStatut() != StatutDossier.ACTIF) {
            throw new ConflitMetierException("Une pathologie ne peut être déclarée que sur un dossier actif");
        }

        return pathologieRepository.save(new Pathologie(
                dossier,
                commande.nom(),
                commande.code(),
                commande.description(),
                commande.dateDiagnosticDeclaree(),
                commande.niveauRisque()
        ));
    }

    public Pathologie modifier(UUID pathologieId, String nom, String code, String description, NiveauRisque niveauRisque) {
        Pathologie pathologie = charger(pathologieId);
        pathologie.modifier(nom, code, description, niveauRisque);
        return pathologie;
    }

    public Pathologie desactiver(UUID pathologieId) {
        Pathologie pathologie = charger(pathologieId);
        pathologie.desactiver();
        return pathologie;
    }

    public Pathologie consulter(UUID pathologieId) {
        return charger(pathologieId);
    }

    private Pathologie charger(UUID pathologieId) {
        return pathologieRepository.findById(pathologieId)
                .orElseThrow(() -> new RessourceIntrouvableException("Pathologie", pathologieId));
    }
}
