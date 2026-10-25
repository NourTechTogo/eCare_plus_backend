package com.nourtech.ecareplus.care.application.service;

import com.nourtech.ecareplus.care.application.command.AjouterTraitementCommand;
import com.nourtech.ecareplus.care.domain.entity.DossierSuivi;
import com.nourtech.ecareplus.care.domain.entity.Traitement;
import com.nourtech.ecareplus.care.domain.enumeration.StatutDossier;
import com.nourtech.ecareplus.care.infrastructure.persistence.DossierSuiviRepository;
import com.nourtech.ecareplus.care.infrastructure.persistence.TraitementRepository;
import com.nourtech.ecareplus.shared.application.exception.ConflitMetierException;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class TraitementService {

    private final TraitementRepository traitementRepository;
    private final DossierSuiviRepository dossierRepository;
    @org.springframework.beans.factory.annotation.Autowired private com.nourtech.ecareplus.communication.application.service.ChangementTraitementService changements;
    @org.springframework.beans.factory.annotation.Autowired private PlanningHistoriqueService historique;

    public TraitementService(TraitementRepository traitementRepository, DossierSuiviRepository dossierRepository) {
        this.traitementRepository = traitementRepository;
        this.dossierRepository = dossierRepository;
    }

    public Traitement ajouter(AjouterTraitementCommand commande) {
        DossierSuivi dossier = dossierRepository.findById(commande.dossierId())
                .orElseThrow(() -> new RessourceIntrouvableException("DossierSuivi", commande.dossierId()));
        if (dossier.getStatut() != StatutDossier.ACTIF) {
            throw new ConflitMetierException("Un traitement ne peut être ajouté que sur un dossier actif");
        }

        var traitement=traitementRepository.save(new Traitement(
                dossier,
                commande.libelle(),
                commande.description(),
                commande.dateDebut(),
                commande.dateFin(),
                commande.prescripteur(),
                commande.consignesGenerales()
        ));
        changements.notifier(traitement);return traitement;
    }

    public Traitement modifier(UUID traitementId, String libelle, String description, LocalDate dateFin, String consignesGenerales) {
        Traitement traitement = charger(traitementId);
        historique.avant(traitement);
        traitement.modifier(libelle, description, dateFin, consignesGenerales);
        historique.apres(traitement);
        changements.notifier(traitement);
        return traitement;
    }

    public Traitement suspendre(UUID traitementId) {
        Traitement traitement = charger(traitementId);
        historique.avant(traitement);
        traitement.suspendre();
        historique.apres(traitement);
        changements.notifier(traitement);
        return traitement;
    }

    public Traitement terminer(UUID traitementId) {
        Traitement traitement = charger(traitementId);
        historique.avant(traitement);
        traitement.terminer();
        historique.apres(traitement);
        changements.notifier(traitement);
        return traitement;
    }

    public Traitement consulter(UUID traitementId) {
        return charger(traitementId);
    }

    private Traitement charger(UUID traitementId) {
        return traitementRepository.findById(traitementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Traitement", traitementId));
    }
}
