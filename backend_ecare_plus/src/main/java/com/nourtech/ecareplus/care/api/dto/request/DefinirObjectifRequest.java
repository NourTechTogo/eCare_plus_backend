package com.nourtech.ecareplus.care.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DefinirObjectifRequest(
        @NotNull UUID medecinId,
        @NotBlank @Size(max = 200) String libelle,
        @Size(max = 5000) String description,
        @DecimalMin("0") BigDecimal cibleMin,
        @DecimalMin("0") BigDecimal cibleMax,
        @Size(max = 30) String unite,
        @NotNull LocalDate dateDebut,
        @FutureOrPresent LocalDate dateFin
) {
}
