package com.nourtech.ecareplus.communication.domain.enumeration;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/** Récurrence structurée choisie par le client lors de la programmation. */
public enum RecurrenceRappel {
    AUCUNE,
    QUOTIDIEN,
    HEBDOMADAIRE,
    MENSUELLE;

    public Instant prochaineOccurrence(Instant occurrenceCourante) {
        ZonedDateTime date = occurrenceCourante.atZone(ZoneOffset.UTC);
        return switch (this) {
            case AUCUNE -> null;
            case QUOTIDIEN -> date.plusDays(1).toInstant();
            case HEBDOMADAIRE -> date.plusWeeks(1).toInstant();
            case MENSUELLE -> date.plusMonths(1).toInstant();
        };
    }
}
