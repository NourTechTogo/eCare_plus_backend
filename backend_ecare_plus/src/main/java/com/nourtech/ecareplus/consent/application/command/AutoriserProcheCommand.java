package com.nourtech.ecareplus.consent.application.command;

import com.nourtech.ecareplus.consent.domain.enumeration.TypeDonneePartage;

import java.time.Instant;
import java.util.UUID;

public record AutoriserProcheCommand(
        UUID patientId,
        UUID procheAidantId,
        UUID consentementId,
        TypeDonneePartage typeDonnee,
        Instant dateDebut,
        Instant dateFin,
        boolean peutConsulter,
        boolean peutRecevoirNotification
) {
}
