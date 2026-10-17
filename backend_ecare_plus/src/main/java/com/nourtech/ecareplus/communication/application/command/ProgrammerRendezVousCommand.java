package com.nourtech.ecareplus.communication.application.command;

import java.time.Instant;
import java.util.UUID;

public record ProgrammerRendezVousCommand(
        UUID patientId,
        String motif,
        Instant dateHeure,
        String lieu,
        String typeRendezVous,
        String commentaire
) {
}
