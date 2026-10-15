package com.nourtech.ecareplus.care.api.dto.response;

import com.nourtech.ecareplus.care.domain.enumeration.StatutDossier;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DossierSuiviResponse(
        UUID id,
        UUID patientId,
        UUID structureId,
        LocalDate dateOuverture,
        StatutDossier statut,
        String resumeMedical,
        String frequenceSuivi,
        LocalDate prochainControle,
        Instant dateDerniereMiseAJour,
        long version
) {
}
