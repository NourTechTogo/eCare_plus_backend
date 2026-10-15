package com.nourtech.ecareplus.identity.api.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record ProfessionnelSanteResponse(
        UtilisateurResponse utilisateur,
        String numeroProfessionnel,
        String specialite,
        String fonction,
        String statutProfessionnel,
        LocalDate datePriseFonction,
        UUID structureId
) {
}
