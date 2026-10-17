package com.nourtech.ecareplus.care.application.command;

import com.nourtech.ecareplus.care.domain.enumeration.NiveauRisque;

import java.time.LocalDate;
import java.util.UUID;

public record DeclarerPathologieCommand(
        UUID dossierId,
        String nom,
        String code,
        String description,
        LocalDate dateDiagnosticDeclaree,
        NiveauRisque niveauRisque
) {
}
