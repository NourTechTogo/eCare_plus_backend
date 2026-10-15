package com.nourtech.ecareplus.auth.application.service;

import com.nourtech.ecareplus.auth.api.dto.ConnexionRequest;
import com.nourtech.ecareplus.auth.api.dto.ProfilAuthentifieResponse;
import com.nourtech.ecareplus.auth.api.dto.SessionResponse;
import com.nourtech.ecareplus.auth.application.exception.AuthentificationException;
import com.nourtech.ecareplus.auth.infrastructure.security.JwtService;
import com.nourtech.ecareplus.identity.application.service.MotDePasseService;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.domain.entity.Utilisateur;
import com.nourtech.ecareplus.identity.domain.enumeration.StatutCompte;
import com.nourtech.ecareplus.identity.infrastructure.persistence.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class AuthentificationService {

    private final UtilisateurRepository utilisateurRepository;
    private final MotDePasseService motDePasseService;
    private final JwtService jwtService;

    public AuthentificationService(
            UtilisateurRepository utilisateurRepository,
            MotDePasseService motDePasseService,
            JwtService jwtService
    ) {
        this.utilisateurRepository = utilisateurRepository;
        this.motDePasseService = motDePasseService;
        this.jwtService = jwtService;
    }

    public SessionResponse connecter(ConnexionRequest request) {
        Utilisateur utilisateur = rechercher(request.identifiant()).orElseThrow(AuthentificationException::new);
        if (utilisateur.getStatutCompte() != StatutCompte.ACTIF
                || !motDePasseService.correspond(request.motDePasse(), utilisateur.getMotDePasseHash())) {
            throw new AuthentificationException();
        }
        utilisateur.enregistrerConnexion(Instant.now());
        JwtService.JetonEmis jeton = jwtService.emettre(utilisateur);
        return new SessionResponse(
                jeton.jeton(),
                "Bearer",
                jeton.expireLe(),
                ProfilAuthentifieResponse.depuis(utilisateur)
        );
    }

    private Optional<Utilisateur> rechercher(String identifiant) {
        return identifiant.contains("@")
                ? utilisateurRepository.findByEmailIgnoreCase(identifiant)
                : utilisateurRepository.findByTelephone(identifiant);
    }
}
