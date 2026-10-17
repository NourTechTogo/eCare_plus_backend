package com.nourtech.ecareplus.identity.application.service;

import com.nourtech.ecareplus.identity.domain.entity.Utilisateur;
import com.nourtech.ecareplus.identity.infrastructure.persistence.UtilisateurRepository;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class GestionCompteService {

    private final UtilisateurRepository utilisateurRepository;

    public GestionCompteService(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    public Utilisateur activer(UUID utilisateurId) {
        Utilisateur utilisateur = charger(utilisateurId);
        utilisateur.activerCompte();
        return utilisateur;
    }

    public Utilisateur suspendre(UUID utilisateurId) {
        Utilisateur utilisateur = charger(utilisateurId);
        utilisateur.suspendreCompte();
        return utilisateur;
    }

    public Utilisateur desactiver(UUID utilisateurId) {
        Utilisateur utilisateur = charger(utilisateurId);
        utilisateur.desactiverCompte();
        return utilisateur;
    }

    public void enregistrerConnexion(UUID utilisateurId, Instant dateConnexion) {
        charger(utilisateurId).enregistrerConnexion(dateConnexion);
    }

    private Utilisateur charger(UUID utilisateurId) {
        return utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur", utilisateurId));
    }
}
