package com.nourtech.ecareplus.care.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

public record ProgrammerPriseRequest(
        @NotNull UUID medicamentId,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal quantite,
        @NotBlank @Size(max = 20) String unite,
        @NotBlank @Size(max = 100) String frequence,
        @NotNull LocalTime heurePrevue,
        @Size(max = 50) String momentPrise,
        @Size(max = 100) String voieAdministration,
        @Size(max = 2000) String consigne
) {
}
