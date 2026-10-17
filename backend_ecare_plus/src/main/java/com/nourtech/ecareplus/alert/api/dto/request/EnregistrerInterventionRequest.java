package com.nourtech.ecareplus.alert.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record EnregistrerInterventionRequest(
        @NotBlank @Size(max = 100) String typeIntervention,
        @Size(max = 5000) String description,
        @NotNull Instant dateIntervention
) {
}
