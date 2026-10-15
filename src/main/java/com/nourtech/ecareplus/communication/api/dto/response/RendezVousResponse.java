package com.nourtech.ecareplus.communication.api.dto.response;

import com.nourtech.ecareplus.communication.domain.enumeration.StatutRendezVous;

import java.time.Instant;
import java.util.UUID;

public record RendezVousResponse(
        UUID id,
        UUID patientId,
        String motif,
        Instant dateHeure,
        String lieu,
        String typeRendezVous,
        StatutRendezVous statut,
        String commentaire,
        long version
) {
}
