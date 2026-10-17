package com.nourtech.ecareplus.care.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record GenererRapportCommand(
        UUID dossierId,
        String titre,
        LocalDate dateDebut,
        LocalDate dateFin,
        String resume,
        String format
) {
}
