package com.nourtech.ecareplus.organization.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreerStructureSanteRequest(
        @NotBlank @Size(max = 200) String nom,
        @NotBlank @Size(max = 80) String typeStructure,
        @NotBlank @Size(max = 300) String adresse,
        @NotBlank @Size(max = 100) String ville,
        @NotBlank @Size(max = 100) String region,
        @NotBlank @Size(max = 30) String telephone,
        @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 100) String numeroAutorisation
) {
}
