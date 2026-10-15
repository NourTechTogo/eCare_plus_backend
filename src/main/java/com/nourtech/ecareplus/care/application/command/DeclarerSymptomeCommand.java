package com.nourtech.ecareplus.care.application.command;

import java.time.Instant;
import java.util.UUID;

public record DeclarerSymptomeCommand(
        UUID patientId,
        String symptome,
        String intensite,
        String description,
        Instant dateApparition,
        boolean persistant
) {
}
