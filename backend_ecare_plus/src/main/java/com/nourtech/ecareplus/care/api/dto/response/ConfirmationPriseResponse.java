package com.nourtech.ecareplus.care.api.dto.response;

import com.nourtech.ecareplus.care.domain.enumeration.StatutPrise;

import java.time.Instant;
import java.util.UUID;

public record ConfirmationPriseResponse(
        UUID id,
        UUID priseMedicamentId,
        Instant datePrevue,
        Instant dateConfirmation,
        StatutPrise statut,
        String commentaire,
        long version
) {
}
