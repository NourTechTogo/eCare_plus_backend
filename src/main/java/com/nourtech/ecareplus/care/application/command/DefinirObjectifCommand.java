package com.nourtech.ecareplus.care.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DefinirObjectifCommand(
        UUID dossierId,
        UUID medecinId,
        String libelle,
        String description,
        BigDecimal cibleMin,
        BigDecimal cibleMax,
        String unite,
        LocalDate dateDebut,
        LocalDate dateFin
) {
}
