package com.nourtech.ecareplus.care.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record OuvrirDossierRequest(
        @NotNull UUID patientId,
        @NotNull UUID structureId,
        @NotBlank @Size(max = 100) String frequenceSuivi
) {
}
