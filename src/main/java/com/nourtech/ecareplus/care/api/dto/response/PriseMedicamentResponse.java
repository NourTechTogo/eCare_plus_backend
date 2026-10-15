package com.nourtech.ecareplus.care.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

public record PriseMedicamentResponse(
        UUID id,
        UUID traitementId,
        UUID medicamentId,
        BigDecimal quantite,
        String unite,
        String frequence,
        LocalTime heurePrevue,
        String momentPrise,
        String voieAdministration,
        String consigne,
        boolean actif,
        long version
) {
}
