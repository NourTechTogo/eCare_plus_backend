package com.nourtech.ecareplus.care.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ModifierMesureRequest(
        @NotNull BigDecimal valeurPrincipale,
        BigDecimal valeurSecondaire,
        @NotBlank @Size(max = 20) String unite,
        @Size(max = 100) String contexteMesure,
        @Size(max = 5000) String commentaire
) {
}
