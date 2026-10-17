package com.nourtech.ecareplus.care.application.command;

import java.time.LocalDate;
import java.util.UUID;

public record AjouterTraitementCommand(
        UUID dossierId,
        String libelle,
        String description,
        LocalDate dateDebut,
        LocalDate dateFin,
        String prescripteur,
        String consignesGenerales
) {
}
