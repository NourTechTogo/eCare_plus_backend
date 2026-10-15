package com.nourtech.ecareplus.consent.api.dto.request;

import com.nourtech.ecareplus.consent.domain.enumeration.TypeDonneePartage;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record AutoriserProcheRequest(
        @NotNull UUID patientId,
        @NotNull UUID procheAidantId,
        @NotNull UUID consentementId,
        @NotNull TypeDonneePartage typeDonnee,
        @NotNull Instant dateDebut,
        Instant dateFin,
        boolean peutConsulter,
        boolean peutRecevoirNotification
) {
}
