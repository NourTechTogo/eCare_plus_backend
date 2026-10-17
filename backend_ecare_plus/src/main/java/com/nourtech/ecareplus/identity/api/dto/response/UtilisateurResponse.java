package com.nourtech.ecareplus.identity.api.dto.response;

import com.nourtech.ecareplus.identity.domain.enumeration.Sexe;
import com.nourtech.ecareplus.identity.domain.enumeration.StatutCompte;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UtilisateurResponse(
        UUID id,
        String nom,
        String prenoms,
        Sexe sexe,
        LocalDate dateNaissance,
        String telephone,
        String email,
        String languePreferee,
        StatutCompte statutCompte,
        Instant derniereConnexion,
        Instant dateCreation,
        Instant dateModification
) {
}
