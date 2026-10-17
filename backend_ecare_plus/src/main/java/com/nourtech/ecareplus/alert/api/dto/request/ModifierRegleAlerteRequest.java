package com.nourtech.ecareplus.alert.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ModifierRegleAlerteRequest(
        @Size(max = 5000) String description,
        @NotBlank @Size(max = 20) String operateur,
        BigDecimal seuilMin,
        BigDecimal seuilMax,
        @NotNull @Positive Integer nombreOccurrences,
        @NotNull @Positive Integer periodeObservation
) {
}
