package com.nourtech.ecareplus.identity.api.dto.response;

import com.nourtech.ecareplus.identity.domain.enumeration.StatutSuivi;

import java.time.LocalDate;

public record PatientResponse(
        UtilisateurResponse utilisateur,
        String numeroPatient,
        String groupeSanguin,
        String adresse,
        String personneAContacter,
        String telephoneUrgence,
        String modeConnexionPrefere,
        LocalDate dateInscription,
        StatutSuivi statutSuivi
) {
}
