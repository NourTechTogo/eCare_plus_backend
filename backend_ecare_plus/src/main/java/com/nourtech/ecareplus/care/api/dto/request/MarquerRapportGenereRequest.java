package com.nourtech.ecareplus.care.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarquerRapportGenereRequest(
        @NotBlank @Size(max = 500) String cheminFichier
) {
}
