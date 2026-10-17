package com.nourtech.ecareplus.identity.api.dto.response;

public record InfirmierResponse(
        ProfessionnelSanteResponse professionnel,
        String service,
        String niveauResponsabilite
) {
}
