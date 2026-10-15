package com.nourtech.ecareplus.identity.api.dto.response;

import java.util.UUID;

public record AdministrateurStructureResponse(
        UtilisateurResponse utilisateur,
        String fonctionAdministrative,
        String niveauAcces,
        UUID structureId
) {
}
