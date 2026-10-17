package com.nourtech.ecareplus.audit.application.service;

import com.nourtech.ecareplus.audit.domain.entity.JournalActivite;
import com.nourtech.ecareplus.audit.infrastructure.persistence.JournalActiviteRepository;
import com.nourtech.ecareplus.identity.domain.entity.Utilisateur;
import com.nourtech.ecareplus.identity.infrastructure.persistence.UtilisateurRepository;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.UUID;

@Service
public class JournalisationService {

    private final JournalActiviteRepository journalRepository;
    private final UtilisateurRepository utilisateurRepository;

    public JournalisationService(
            JournalActiviteRepository journalRepository,
            UtilisateurRepository utilisateurRepository
    ) {
        this.journalRepository = journalRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Transactional
    public JournalActivite enregistrer(
            UUID utilisateurId,
            String action,
            String ressource,
            UUID ressourceId,
            String adresseIp,
            String appareil,
            String resultat
    ) {
        Utilisateur utilisateur = utilisateurId == null
                ? null
                : utilisateurRepository.findById(utilisateurId)
                        .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur", utilisateurId));
        return journalRepository.save(new JournalActivite(
                utilisateur,
                action,
                ressource,
                ressourceId,
                adresseIp,
                appareil,
                resultat
        ));
    }

    /**
     * Enregistre une action à partir de l'identifiant porté par le principal
     * authentifié. Un principal absent produit volontairement une entrée système,
     * sans attribuer l'action à la ressource métier concernée.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JournalActivite enregistrerPourPrincipal(
            String principal,
            String action,
            String ressource,
            UUID ressourceId,
            String adresseIp,
            String appareil,
            String resultat
    ) {
        Utilisateur utilisateur = trouverUtilisateur(principal);
        return journalRepository.save(new JournalActivite(
                utilisateur,
                action,
                ressource,
                ressourceId,
                adresseIp,
                appareil,
                resultat
        ));
    }

    /**
     * Les échecs doivent survivre au rollback du cas d'usage ayant échoué.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JournalActivite enregistrerEchec(
            String principal,
            String action,
            String ressource,
            UUID ressourceId,
            String adresseIp,
            String appareil
    ) {
        return enregistrerPourPrincipal(
                principal, action, ressource, ressourceId, adresseIp, appareil, "ECHEC"
        );
    }

    private Utilisateur trouverUtilisateur(String principal) {
        if (principal == null || principal.isBlank() || "anonymousUser".equals(principal)) {
            return null;
        }
        try {
            return utilisateurRepository.findById(UUID.fromString(principal)).orElse(null);
        } catch (IllegalArgumentException ignored) {
            return utilisateurRepository.findByEmailIgnoreCase(principal).orElse(null);
        }
    }
}
