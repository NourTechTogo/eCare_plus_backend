package com.nourtech.ecareplus.care.api.dto.request;

import com.nourtech.ecareplus.care.domain.enumeration.NiveauRisque;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModifierPathologieRequest(
        @NotBlank @Size(max = 150) String nom,
        @Size(max = 40) String code,
        @Size(max = 5000) String description,
        NiveauRisque niveauRisque
) {
}
