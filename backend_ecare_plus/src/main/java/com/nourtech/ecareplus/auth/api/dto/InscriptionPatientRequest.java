package com.nourtech.ecareplus.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InscriptionPatientRequest(
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 150) String prenoms,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 30) String telephone,
        @NotBlank @Size(min = 8, max = 128) String motDePasse
) {
    public InscriptionPatientRequest {
        nom = nettoyer(nom);
        prenoms = nettoyer(prenoms);
        email = nettoyer(email);
        telephone = nettoyer(telephone);
    }

    private static String nettoyer(String valeur) {
        return valeur == null ? null : valeur.trim();
    }
}
