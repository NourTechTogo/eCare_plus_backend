package com.nourtech.ecareplus.communication.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModifierContenuEducatifRequest(
        @NotBlank @Size(max = 200) String titre,
        @Size(max = 500) String resume,
        @NotBlank @Size(max = 20000) String contenu,
        @Size(max = 500) String urlMedia
) {
}
