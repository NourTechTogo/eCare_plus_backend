package com.nourtech.ecareplus.care.api.dto.response;

import com.nourtech.ecareplus.care.domain.enumeration.StatutRapport;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RapportSuiviResponse(
        UUID id,
        UUID dossierId,
        String titre,
        LocalDate dateDebut,
        LocalDate dateFin,
        String resume,
        Instant dateGeneration,
        String format,
        String cheminFichier,
        StatutRapport statut,
        long version
) {
}
