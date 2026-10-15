package com.nourtech.ecareplus.alert.api.dto.response;

import com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite;
import com.nourtech.ecareplus.alert.domain.enumeration.OrigineAlerte;
import com.nourtech.ecareplus.alert.domain.enumeration.StatutAlerte;
import com.nourtech.ecareplus.alert.domain.enumeration.TypeAlerte;

import java.time.Instant;
import java.util.UUID;

public record AlerteResponse(
        UUID id,
        UUID patientId,
        UUID mesureId,
        UUID regleId,
        UUID professionnelId,
        TypeAlerte typeAlerte,
        OrigineAlerte origine,
        String titre,
        String description,
        NiveauPriorite niveauPriorite,
        StatutAlerte statut,
        Instant datePriseEnCharge,
        Instant dateCloture,
        String motifCloture,
        long version
) {
}
