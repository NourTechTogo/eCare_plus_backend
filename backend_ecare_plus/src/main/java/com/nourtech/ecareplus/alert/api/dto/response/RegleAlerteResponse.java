package com.nourtech.ecareplus.alert.api.dto.response;

import com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite;
import com.nourtech.ecareplus.care.domain.enumeration.TypeMesure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RegleAlerteResponse(
        UUID id,
        String nom,
        String description,
        TypeMesure typeMesure,
        String operateur,
        BigDecimal seuilMin,
        BigDecimal seuilMax,
        Integer nombreOccurrences,
        Integer periodeObservation,
        NiveauPriorite niveauPriorite,
        boolean actif,
        Instant dateValidation,
        long version,
        UUID patientId,
        UUID structureId,
        String composante,
        String unite,
        String contexteMesure
) {
}
