package com.nourtech.ecareplus.consent.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RetirerConsentementRequest(@NotNull Instant dateRetrait) {
}
