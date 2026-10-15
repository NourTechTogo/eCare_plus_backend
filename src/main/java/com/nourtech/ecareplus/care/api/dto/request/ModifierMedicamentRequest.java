package com.nourtech.ecareplus.care.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModifierMedicamentRequest(
        @NotBlank @Size(max = 150) String nomCommercial,
        @NotBlank @Size(max = 200) String denomination,
        @NotBlank @Size(max = 60) String forme,
        @NotBlank @Size(max = 40) String dosage,
        @NotBlank @Size(max = 20) String uniteDosage
) {
}
