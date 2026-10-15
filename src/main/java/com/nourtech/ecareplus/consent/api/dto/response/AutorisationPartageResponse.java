package com.nourtech.ecareplus.consent.api.dto.response;

import com.nourtech.ecareplus.consent.domain.enumeration.StatutAutorisation;
import com.nourtech.ecareplus.consent.domain.enumeration.TypeDonneePartage;

import java.time.Instant;
import java.util.UUID;

public record AutorisationPartageResponse(
        UUID id,
        UUID patientId,
        UUID procheAidantId,
        UUID consentementId,
        TypeDonneePartage typeDonnee,
        Instant dateDebut,
        Instant dateFin,
        boolean peutConsulter,
        boolean peutRecevoirNotification,
        StatutAutorisation statut
) {
}
