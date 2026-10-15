package com.nourtech.ecareplus.subscription.api.dto.response;

import com.nourtech.ecareplus.subscription.domain.enumeration.FrequencePaiement;
import com.nourtech.ecareplus.subscription.domain.enumeration.StatutAbonnement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AbonnementStructureResponse(
        UUID id,
        UUID structureId,
        String formule,
        LocalDate dateDebut,
        LocalDate dateFin,
        int nombrePatientsAutorises,
        BigDecimal montant,
        StatutAbonnement statut,
        FrequencePaiement frequencePaiement
) {
}
