package com.nourtech.ecareplus.communication.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ReporterRappelRequest(
        @NotNull Instant nouvelleDateHeurePrevue
) {
}
