package com.nourtech.ecareplus.identity.api.dto.response;

public record MedecinResponse(
        ProfessionnelSanteResponse professionnel,
        String numeroOrdre,
        String domaineSpecialisation
) {
}
