package com.nourtech.ecareplus.organization.application.command;

public record CreerStructureSanteCommand(
        String nom,
        String typeStructure,
        String adresse,
        String ville,
        String region,
        String telephone,
        String email,
        String numeroAutorisation
) {
}
