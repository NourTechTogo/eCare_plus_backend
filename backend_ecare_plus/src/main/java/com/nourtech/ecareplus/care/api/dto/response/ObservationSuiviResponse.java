package com.nourtech.ecareplus.care.api.dto.response;

import java.util.UUID;

public record ObservationSuiviResponse(
        UUID id,
        UUID dossierId,
        UUID professionnelId,
        String contenu,
        String typeObservation,
        boolean confidentielle,
        boolean archivee,
        long version
) {
}
