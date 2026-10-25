package com.nourtech.ecareplus.care.application.command;

import com.nourtech.ecareplus.care.domain.enumeration.SourceMesure;
import com.nourtech.ecareplus.care.domain.enumeration.TypeMesure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EnregistrerMesureCommand(
        UUID dossierId,
        UUID patientId,
        TypeMesure typeMesure,
        BigDecimal valeurPrincipale,
        BigDecimal valeurSecondaire,
        String unite,
        Instant dateMesure,
        String contexteMesure,
        String commentaire,
        SourceMesure source,
        String statutSynchronisation,
        String sourceRef
) {
    public EnregistrerMesureCommand(UUID dossierId, UUID patientId, TypeMesure typeMesure,
                                   BigDecimal valeurPrincipale, BigDecimal valeurSecondaire, String unite,
                                   Instant dateMesure, String contexteMesure, String commentaire,
                                   SourceMesure source, String statutSynchronisation) {
        this(dossierId, patientId, typeMesure, valeurPrincipale, valeurSecondaire, unite, dateMesure,
                contexteMesure, commentaire, source, statutSynchronisation, null);
    }
}
