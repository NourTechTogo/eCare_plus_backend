package com.nourtech.ecareplus.subscription.application.command;

import com.nourtech.ecareplus.subscription.domain.enumeration.FrequencePaiement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreerAbonnementCommand(
        UUID structureId,
        String formule,
        LocalDate dateDebut,
        LocalDate dateFin,
        int nombrePatientsAutorises,
        BigDecimal montant,
        FrequencePaiement frequencePaiement
) {
}
