package com.nourtech.ecareplus.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InscriptionPatientRequest(
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 150) String prenoms,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 30) String telephone,
        @NotBlank @Size(min = 12, max = 72) @jakarta.validation.constraints.Pattern(regexp="(?s)(?=.*[A-Za-z])(?=.*[0-9]).+") String motDePasse,
        Boolean consentementAccepte,
        @Size(max=40) String versionDocument,
        Boolean cguLues,
        Boolean confidentialiteLue
) {
    public InscriptionPatientRequest(String nom,String prenoms,String email,String telephone,String motDePasse){this(nom,prenoms,email,telephone,motDePasse,false,null,false,false);}
    public InscriptionPatientRequest {
        consentementAccepte = Boolean.TRUE.equals(consentementAccepte);
        cguLues = Boolean.TRUE.equals(cguLues);
        confidentialiteLue = Boolean.TRUE.equals(confidentialiteLue);
        nom = nettoyer(nom);
        prenoms = nettoyer(prenoms);
        email = nettoyer(email);
        telephone = nettoyer(telephone);
    }

    private static String nettoyer(String valeur) {
        return valeur == null ? null : valeur.trim();
    }
}

