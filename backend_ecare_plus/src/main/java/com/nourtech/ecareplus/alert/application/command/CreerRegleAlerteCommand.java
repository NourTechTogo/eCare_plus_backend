package com.nourtech.ecareplus.alert.application.command;

import com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite;
import com.nourtech.ecareplus.care.domain.enumeration.TypeMesure;

import java.math.BigDecimal;
import java.util.UUID;

public record CreerRegleAlerteCommand(
        String nom,
        String description,
        TypeMesure typeMesure,
        String operateur,
        BigDecimal seuilMin,
        BigDecimal seuilMax,
        Integer nombreOccurrences,
        Integer periodeObservation,
        NiveauPriorite niveauPriorite,
        UUID patientId,
        String composante,
        String unite,
        String contexteMesure
) {
    public CreerRegleAlerteCommand(String nom, String description, TypeMesure typeMesure, String operateur,
                                  BigDecimal seuilMin, BigDecimal seuilMax, Integer nombreOccurrences,
                                  Integer periodeObservation, NiveauPriorite niveauPriorite) {
        this(nom, description, typeMesure, operateur, seuilMin, seuilMax, nombreOccurrences, periodeObservation,
                niveauPriorite, null, "PRINCIPALE", null, null);
    }
}
