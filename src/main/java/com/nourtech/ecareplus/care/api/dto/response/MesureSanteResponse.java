package com.nourtech.ecareplus.care.api.dto.response;

import com.nourtech.ecareplus.care.domain.enumeration.SourceMesure;
import com.nourtech.ecareplus.care.domain.enumeration.TypeMesure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MesureSanteResponse(
        UUID id,
        UUID patientId,
        UUID dossierId,
        TypeMesure typeMesure,
        BigDecimal valeurPrincipale,
        BigDecimal valeurSecondaire,
        String unite,
        Instant dateMesure,
        String contexteMesure,
        String commentaire,
        SourceMesure source,
        String statutSynchronisation,
        boolean valeurConfirmee,
        long version
) {
}
