package com.nourtech.ecareplus.care.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AjouterObservationRequest(
        @NotNull UUID professionnelId,
        @NotBlank @Size(max = 5000) String contenu,
        @NotBlank @Size(max = 50) String typeObservation,
        boolean confidentielle
) {
}
