package com.nourtech.ecareplus.care.application.command;

import java.time.Instant;
import java.util.UUID;

public record ProgrammerConfirmationPriseCommand(
        UUID priseMedicamentId,
        Instant datePrevue
) {
}
