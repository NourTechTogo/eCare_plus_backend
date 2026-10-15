package com.nourtech.ecareplus.identity.application.service;

import com.nourtech.ecareplus.identity.infrastructure.persistence.UtilisateurRepository;
import com.nourtech.ecareplus.shared.application.exception.ConflitMetierException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VerificationUniciteCompteService {

    private final UtilisateurRepository utilisateurRepository;

    public VerificationUniciteCompteService(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    public void verifier(String email, String telephone) {
        if (email != null && !email.isBlank() && utilisateurRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new ConflitMetierException("Cette adresse e-mail est déjà utilisée");
        }
        if (telephone != null && !telephone.isBlank() && utilisateurRepository.existsByTelephone(telephone.trim())) {
            throw new ConflitMetierException("Ce numéro de téléphone est déjà utilisé");
        }
    }
}
