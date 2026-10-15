package com.nourtech.ecareplus.alert.application.command;

import com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite;
import com.nourtech.ecareplus.alert.domain.enumeration.TypeAlerte;

import java.util.UUID;

public record GenererAlerteCommand(
        UUID patientId,
        UUID mesureId,
        UUID regleId,
        TypeAlerte typeAlerte,
        String titre,
        String description,
        NiveauPriorite niveauPriorite
) {
}
