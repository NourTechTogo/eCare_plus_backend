package com.nourtech.ecareplus.alert.application.command;

import com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite;
import com.nourtech.ecareplus.care.domain.enumeration.TypeMesure;

import java.math.BigDecimal;

public record CreerRegleAlerteCommand(
        String nom,
        String description,
        TypeMesure typeMesure,
        String operateur,
        BigDecimal seuilMin,
        BigDecimal seuilMax,
        Integer nombreOccurrences,
        Integer periodeObservation,
        NiveauPriorite niveauPriorite
) {
}
