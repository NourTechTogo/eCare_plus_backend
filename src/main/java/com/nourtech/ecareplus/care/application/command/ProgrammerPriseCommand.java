package com.nourtech.ecareplus.care.application.command;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

public record ProgrammerPriseCommand(
        UUID traitementId,
        UUID medicamentId,
        BigDecimal quantite,
        String unite,
        String frequence,
        LocalTime heurePrevue,
        String momentPrise,
        String voieAdministration,
        String consigne
) {
}
