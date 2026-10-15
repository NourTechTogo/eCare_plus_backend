package com.nourtech.ecareplus.consent.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record AccorderConsentementRequest(
        @NotNull UUID patientId,
        @NotBlank @Size(max = 80) String typeConsentement,
        @NotBlank @Size(max = 10_000) String description,
        @NotBlank @Size(max = 40) String versionDocument,
        @NotNull Instant dateAcceptation
) {
}
