package com.nourtech.ecareplus.communication.api.dto.response;

import com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification;
import com.nourtech.ecareplus.communication.domain.enumeration.RecurrenceRappel;
import com.nourtech.ecareplus.communication.domain.enumeration.StatutRappel;
import com.nourtech.ecareplus.communication.domain.enumeration.TypeRappel;

import java.time.Instant;
import java.util.UUID;

public record RappelResponse(
        UUID id,
        UUID patientId,
        UUID priseMedicamentId,
        TypeRappel typeRappel,
        String titre,
        String message,
        Instant dateHeurePrevue,
        RecurrenceRappel frequence,
        StatutRappel statut,
        CanalNotification canal,
        Instant dateFin,
        Instant derniereExecution,
        long version
) {
}
