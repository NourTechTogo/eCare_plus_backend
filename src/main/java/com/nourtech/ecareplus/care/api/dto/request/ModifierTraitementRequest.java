package com.nourtech.ecareplus.care.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ModifierTraitementRequest(
        @NotBlank @Size(max = 200) String libelle,
        @Size(max = 5000) String description,
        LocalDate dateFin,
        @Size(max = 5000) String consignesGenerales
) {
}
