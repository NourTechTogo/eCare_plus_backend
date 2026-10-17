package com.nourtech.ecareplus.communication.application.service;

import com.nourtech.ecareplus.communication.application.command.CreerContenuEducatifCommand;
import com.nourtech.ecareplus.communication.domain.entity.ContenuEducatif;
import com.nourtech.ecareplus.communication.infrastructure.persistence.ContenuEducatifRepository;
import com.nourtech.ecareplus.identity.domain.entity.Medecin;
import com.nourtech.ecareplus.identity.infrastructure.persistence.MedecinRepository;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class ContenuEducatifService {

    private final ContenuEducatifRepository contenuRepository;
    private final MedecinRepository medecinRepository;

    public ContenuEducatifService(ContenuEducatifRepository contenuRepository, MedecinRepository medecinRepository) {
        this.contenuRepository = contenuRepository;
        this.medecinRepository = medecinRepository;
    }

    public ContenuEducatif creer(CreerContenuEducatifCommand commande) {
        Medecin auteur = medecinRepository.findById(commande.auteurId())
                .orElseThrow(() -> new RessourceIntrouvableException("Medecin", commande.auteurId()));

        return contenuRepository.save(new ContenuEducatif(
                auteur,
                commande.titre(),
                commande.resume(),
                commande.contenu(),
                commande.typeContenu(),
                commande.langue(),
                commande.pathologieCible(),
                commande.urlMedia()
        ));
    }

    public ContenuEducatif modifier(UUID contenuId, String titre, String resume, String contenu, String urlMedia) {
        ContenuEducatif contenuEducatif = charger(contenuId);
        contenuEducatif.modifierContenu(titre, resume, contenu, urlMedia);
        return contenuEducatif;
    }

    public ContenuEducatif valider(UUID contenuId) {
        ContenuEducatif contenuEducatif = charger(contenuId);
        contenuEducatif.validerContenu();
        return contenuEducatif;
    }

    public ContenuEducatif publier(UUID contenuId, Instant maintenant) {
        ContenuEducatif contenuEducatif = charger(contenuId);
        contenuEducatif.publier(maintenant);
        return contenuEducatif;
    }

    public ContenuEducatif archiver(UUID contenuId) {
        ContenuEducatif contenuEducatif = charger(contenuId);
        contenuEducatif.archiver();
        return contenuEducatif;
    }

    public ContenuEducatif consulter(UUID contenuId) {
        return charger(contenuId);
    }

    private ContenuEducatif charger(UUID contenuId) {
        return contenuRepository.findById(contenuId)
                .orElseThrow(() -> new RessourceIntrouvableException("ContenuEducatif", contenuId));
    }
}
