package com.nourtech.ecareplus.communication.api.dto.response;

import com.nourtech.ecareplus.communication.domain.enumeration.StatutValidationContenu;

import java.time.Instant;
import java.util.UUID;

public record ContenuEducatifResponse(
        UUID id,
        UUID auteurId,
        String titre,
        String resume,
        String contenu,
        String typeContenu,
        String langue,
        String pathologieCible,
        StatutValidationContenu statutValidation,
        Instant datePublication,
        String urlMedia,
        long version
) {
}
