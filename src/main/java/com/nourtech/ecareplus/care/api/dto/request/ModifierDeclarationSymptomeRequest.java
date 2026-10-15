package com.nourtech.ecareplus.care.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModifierDeclarationSymptomeRequest(
        @NotBlank @Size(max = 150) String symptome,
        @NotBlank @Size(max = 30) String intensite,
        @Size(max = 5000) String description,
        boolean persistant
) {
}
