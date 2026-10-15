package com.nourtech.ecareplus.care.api.dto.response;

import com.nourtech.ecareplus.care.domain.enumeration.StatutTraitement;

import java.time.LocalDate;
import java.util.UUID;

public record TraitementResponse(
        UUID id,
        UUID dossierId,
        String libelle,
        String description,
        LocalDate dateDebut,
        LocalDate dateFin,
        StatutTraitement statut,
        String prescripteur,
        String consignesGenerales,
        long version
) {
}
