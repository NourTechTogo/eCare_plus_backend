package com.nourtech.ecareplus.sync.application.service;

import com.nourtech.ecareplus.sync.application.command.RecevoirSynchronisationCommand;
import com.nourtech.ecareplus.identity.domain.entity.Utilisateur;
import com.nourtech.ecareplus.identity.infrastructure.persistence.UtilisateurRepository;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import com.nourtech.ecareplus.sync.domain.entity.ElementSynchronisation;
import com.nourtech.ecareplus.sync.infrastructure.persistence.ElementSynchronisationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class SynchronisationService {

    private final ElementSynchronisationRepository synchronisationRepository;
    private final UtilisateurRepository utilisateurRepository;

    public SynchronisationService(
            ElementSynchronisationRepository synchronisationRepository,
            UtilisateurRepository utilisateurRepository
    ) {
        this.synchronisationRepository = synchronisationRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    public ElementSynchronisation recevoir(
            UUID utilisateurId,
            String typeRessource,
            String identifiantAppareil,
            String identifiantLocal,
            Map<String, Object> contenu,
            Instant dateCreationLocale
    ) {
        return synchronisationRepository
                .findByUtilisateurIdAndIdentifiantAppareilAndIdentifiantLocal(
                        utilisateurId,
                        identifiantAppareil,
                        identifiantLocal
                )
                .orElseGet(() -> creer(
                        utilisateurId,
                        typeRessource,
                        identifiantAppareil,
                        identifiantLocal,
                        contenu,
                        dateCreationLocale
                ));
    }

    public ElementSynchronisation recevoir(RecevoirSynchronisationCommand command) {
        return recevoir(
                command.utilisateurId(), command.typeRessource(), command.identifiantAppareil(),
                command.identifiantLocal(), command.contenu(), command.dateCreationLocale()
        );
    }

    private ElementSynchronisation creer(
            UUID utilisateurId,
            String typeRessource,
            String identifiantAppareil,
            String identifiantLocal,
            Map<String, Object> contenu,
            Instant dateCreationLocale
    ) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur", utilisateurId));
        return synchronisationRepository.save(new ElementSynchronisation(
                utilisateur,
                typeRessource,
                identifiantAppareil,
                identifiantLocal,
                contenu,
                dateCreationLocale
        ));
    }
}
