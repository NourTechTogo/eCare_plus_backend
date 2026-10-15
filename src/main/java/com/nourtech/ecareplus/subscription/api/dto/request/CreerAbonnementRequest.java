package com.nourtech.ecareplus.subscription.api.dto.request;

import com.nourtech.ecareplus.subscription.domain.enumeration.FrequencePaiement;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreerAbonnementRequest(
        @NotNull UUID structureId,
        @NotBlank @Size(max = 80) String formule,
        @NotNull LocalDate dateDebut,
        @NotNull @FutureOrPresent LocalDate dateFin,
        @Positive int nombrePatientsAutorises,
        @NotNull @DecimalMin("0.00") BigDecimal montant,
        @NotNull FrequencePaiement frequencePaiement
) {
}
