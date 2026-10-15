package com.nourtech.ecareplus.care.api.dto.response;

import com.nourtech.ecareplus.care.domain.enumeration.NiveauRisque;
import com.nourtech.ecareplus.care.domain.enumeration.StatutPathologie;

import java.time.LocalDate;
import java.util.UUID;

public record PathologieResponse(
        UUID id,
        UUID dossierId,
        String nom,
        String code,
        String description,
        LocalDate dateDiagnosticDeclaree,
        StatutPathologie statut,
        NiveauRisque niveauRisque,
        long version
) {
}
