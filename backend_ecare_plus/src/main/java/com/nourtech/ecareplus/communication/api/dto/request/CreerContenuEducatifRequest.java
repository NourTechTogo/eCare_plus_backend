package com.nourtech.ecareplus.communication.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreerContenuEducatifRequest(
        @NotNull UUID auteurId,
        @NotBlank @Size(max = 200) String titre,
        @Size(max = 500) String resume,
        @NotBlank @Size(max = 20000) String contenu,
        @NotBlank @Size(max = 50) String typeContenu,
        @NotBlank @Size(max = 10) String langue,
        @Size(max = 150) String pathologieCible,
        @Size(max = 500) String urlMedia
) {
}
