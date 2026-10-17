package com.nourtech.ecareplus.care.api.dto.request;

import com.nourtech.ecareplus.care.domain.enumeration.SourceMesure;
import com.nourtech.ecareplus.care.domain.enumeration.TypeMesure;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EnregistrerMesureRequest(
        @NotNull UUID patientId,
        @NotNull TypeMesure typeMesure,
        @NotNull BigDecimal valeurPrincipale,
        BigDecimal valeurSecondaire,
        @NotBlank @Size(max = 20) String unite,
        @NotNull @PastOrPresent Instant dateMesure,
        @Size(max = 100) String contexteMesure,
        @Size(max = 5000) String commentaire,
        @NotNull SourceMesure source,
        @Size(max = 30) String statutSynchronisation
) {
}
