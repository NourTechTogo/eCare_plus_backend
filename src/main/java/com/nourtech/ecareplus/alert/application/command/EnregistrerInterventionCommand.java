package com.nourtech.ecareplus.alert.application.command;

import java.time.Instant;
import java.util.UUID;

public record EnregistrerInterventionCommand(
        UUID alerteId,
        String typeIntervention,
        String description,
        Instant dateIntervention
) {
}
