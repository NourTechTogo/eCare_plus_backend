package com.nourtech.ecareplus.identity.application.command;

import com.nourtech.ecareplus.identity.domain.enumeration.Sexe;

import java.time.LocalDate;
import java.util.UUID;

public record CreerMedecinCommand(
        String numeroProfessionnel,
        String numeroOrdre,
        String nom,
        String prenoms,
        Sexe sexe,
        LocalDate dateNaissance,
        String telephone,
        String email,
        String motDePasseHash,
        String languePreferee,
        String specialite,
        String domaineSpecialisation,
        LocalDate datePriseFonction,
        UUID structureId
) {
}
