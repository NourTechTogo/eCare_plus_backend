package com.nourtech.ecareplus.care.application.command;

import java.util.UUID;

public record OuvrirDossierCommand(
        UUID patientId,
        UUID structureId,
        String frequenceSuivi
) {
}
