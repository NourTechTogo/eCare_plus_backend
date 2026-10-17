package com.nourtech.ecareplus.communication.application.command;

import com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification;
import com.nourtech.ecareplus.communication.domain.enumeration.RecurrenceRappel;
import com.nourtech.ecareplus.communication.domain.enumeration.TypeRappel;

import java.time.Instant;
import java.util.UUID;

public record ProgrammerRappelCommand(
        UUID patientId,
        UUID priseMedicamentId,
        TypeRappel typeRappel,
        String titre,
        String message,
        Instant dateHeurePrevue,
        RecurrenceRappel frequence,
        CanalNotification canal,
        Instant dateFin
) {
}
