package com.nourtech.ecareplus.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConnexionRequest(
        @NotBlank @Size(max = 254) String identifiant,
        @NotBlank @Size(max = 128) String motDePasse
) {
    public ConnexionRequest {
        identifiant = identifiant == null ? null : identifiant.trim();
    }
}
