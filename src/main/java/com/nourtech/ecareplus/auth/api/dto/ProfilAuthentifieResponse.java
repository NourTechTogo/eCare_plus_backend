package com.nourtech.ecareplus.auth.api.dto;

import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.domain.entity.Utilisateur;
import com.nourtech.ecareplus.identity.domain.enumeration.StatutCompte;

import java.util.UUID;

public record ProfilAuthentifieResponse(
        UUID id,
        String numeroPatient,
        String nom,
        String prenoms,
        String email,
        String telephone,
        StatutCompte statutCompte
) {
    public static ProfilAuthentifieResponse depuis(Utilisateur utilisateur) {
        return new ProfilAuthentifieResponse(
                utilisateur.getId(),
                utilisateur instanceof Patient patient ? patient.getNumeroPatient() : null,
                utilisateur.getNom(),
                utilisateur.getPrenoms(),
                utilisateur.getEmail(),
                utilisateur.getTelephone(),
                utilisateur.getStatutCompte()
        );
    }
}
