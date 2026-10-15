package com.nourtech.ecareplus.care.api.dto.response;

import com.nourtech.ecareplus.care.domain.enumeration.StatutObjectif;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ObjectifSuiviResponse(
        UUID id,
        UUID dossierId,
        UUID medecinId,
        String libelle,
        String description,
        BigDecimal valeurCibleMin,
        BigDecimal valeurCibleMax,
        String unite,
        LocalDate dateDebut,
        LocalDate dateFin,
        StatutObjectif statut,
        long version
) {
}
