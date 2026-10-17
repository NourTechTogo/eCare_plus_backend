package com.nourtech.ecareplus.organization.api.dto.response;

import com.nourtech.ecareplus.organization.domain.enumeration.StatutStructure;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StructureSanteResponse(
        UUID id,
        String nom,
        String typeStructure,
        String adresse,
        String ville,
        String region,
        String telephone,
        String email,
        StatutStructure statut,
        String numeroAutorisation,
        LocalDate dateAdhesion,
        long version,
        Instant dateCreation,
        Instant dateModification
) {
}
