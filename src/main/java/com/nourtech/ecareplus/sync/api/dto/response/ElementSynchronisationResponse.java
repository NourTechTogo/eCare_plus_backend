package com.nourtech.ecareplus.sync.api.dto.response;

import com.nourtech.ecareplus.sync.domain.enumeration.StatutSynchronisation;

import java.time.Instant;
import java.util.UUID;

public record ElementSynchronisationResponse(
        UUID id,
        UUID utilisateurId,
        String typeRessource,
        String identifiantAppareil,
        String identifiantLocal,
        Instant dateCreationLocale,
        int nombreTentatives,
        StatutSynchronisation statut,
        String messageErreur
) {
}
