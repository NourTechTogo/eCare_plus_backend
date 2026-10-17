package com.nourtech.ecareplus.care.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record ModifierHorairePriseRequest(
        @NotNull LocalTime heurePrevue,
        @NotBlank @Size(max = 100) String frequence,
        @Size(max = 50) String momentPrise
) {
}
