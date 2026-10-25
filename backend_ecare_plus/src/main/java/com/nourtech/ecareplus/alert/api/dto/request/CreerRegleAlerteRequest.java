package com.nourtech.ecareplus.alert.api.dto.request;

import com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite;
import com.nourtech.ecareplus.care.domain.enumeration.TypeMesure;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.Pattern;

public record CreerRegleAlerteRequest(
        @NotBlank @Size(max = 150) String nom,
        @Size(max = 5000) String description,
        @NotNull TypeMesure typeMesure,
        @NotBlank @Size(max = 20) String operateur,
        BigDecimal seuilMin,
        BigDecimal seuilMax,
        @NotNull @Positive Integer nombreOccurrences,
        @NotNull @Positive Integer periodeObservation,
        @NotNull NiveauPriorite niveauPriorite,
        UUID patientId,
        @Pattern(regexp = "PRINCIPALE|DIASTOLIQUE") String composante,
        @Size(max = 20) String unite,
        @Size(max = 100) String contexteMesure
) {
}
