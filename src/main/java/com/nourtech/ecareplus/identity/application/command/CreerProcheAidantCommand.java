package com.nourtech.ecareplus.identity.application.command;

import com.nourtech.ecareplus.identity.domain.enumeration.Sexe;

import java.time.LocalDate;

public record CreerProcheAidantCommand(
        String nom,
        String prenoms,
        Sexe sexe,
        LocalDate dateNaissance,
        String telephone,
        String email,
        String motDePasseHash,
        String languePreferee,
        String lienAvecPatient
) {
}
