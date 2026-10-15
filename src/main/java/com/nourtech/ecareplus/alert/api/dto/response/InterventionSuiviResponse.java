package com.nourtech.ecareplus.alert.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record InterventionSuiviResponse(
        UUID id,
        UUID alerteId,
        String typeIntervention,
        String description,
        Instant dateIntervention,
        String resultat,
        String actionSuivante,
        Instant dateProchaineAction,
        long version
) {
}
