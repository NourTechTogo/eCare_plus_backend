package com.nourtech.ecareplus.consent.application.command;

import java.time.Instant;
import java.util.UUID;

public record AccorderConsentementCommand(
        UUID patientId,
        String typeConsentement,
        String description,
        String versionDocument,
        String adresseIp,
        Instant dateAcceptation
) {
}
