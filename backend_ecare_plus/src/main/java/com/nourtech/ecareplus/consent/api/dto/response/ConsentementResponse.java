package com.nourtech.ecareplus.consent.api.dto.response;

import com.nourtech.ecareplus.consent.domain.enumeration.StatutConsentement;

import java.time.Instant;
import java.util.UUID;

public record ConsentementResponse(
        UUID id,
        UUID patientId,
        String typeConsentement,
        String description,
        Instant dateAcceptation,
        Instant dateRetrait,
        StatutConsentement statut,
        String versionDocument
) {
}
