package com.nourtech.ecareplus.care.application.command;

import java.util.UUID;

public record AjouterObservationCommand(
        UUID dossierId,
        UUID professionnelId,
        String contenu,
        String typeObservation,
        boolean confidentielle
) {
}
